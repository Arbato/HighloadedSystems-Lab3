package ru.itmo.gateway.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.http.MediaType as HttpMediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import org.slf4j.LoggerFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ArrayNode


@RestController
class SwaggerAggregatorController {

    @Autowired
    private lateinit var routeLocator: RouteLocator

    @Autowired
    private lateinit var webClient: WebClient

    private val logger = LoggerFactory.getLogger(SwaggerAggregatorController::class.java)
    private val objectMapper = ObjectMapper()


    @GetMapping(value = ["/v1/api-docs"], produces = [HttpMediaType.APPLICATION_JSON_VALUE])
    fun aggregateApiDocs(): Mono<ObjectNode> {
        return Flux.from(routeLocator.routes)
            .filter { route ->
                route.id != null && !route.id.contains("null")
            }
            .flatMap { route ->
                fetchServiceDocs(route.id)
                    .map { docs -> route.id to docs }
                    .onErrorResume { error ->
                        logger.warn("⚠️ Skipping ${route.id}: ${error.message}")
                        Mono.empty()
                    }
            }
            .collectMap({ it.first }, { it.second })
            .map { serviceDocs ->
                mergeAllApiDocs(serviceDocs)
            }
            .onErrorResume { error ->
                logger.error("❌ Error aggregating docs: ${error.message}", error)
                Mono.just(objectMapper.createObjectNode())
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun fetchServiceDocs(serviceId: String?): Mono<ObjectNode> {
        if (serviceId == null) return Mono.empty()
        val port = 8080

        val docsUrl = "http://$serviceId:$port/v1/api-docs"
        logger.info("📡 Fetching docs from: $docsUrl")

        return webClient.get()
            .uri(docsUrl)
            .retrieve()
            .bodyToMono(String::class.java)
            .map { json -> objectMapper.readTree(json) as ObjectNode }
            .doOnSuccess { docs ->
                val pathsCount = docs.get("paths")?.size() ?: 0
                logger.info("✅ Successfully fetched $pathsCount paths from $serviceId")
            }
            .onErrorResume { error ->
                logger.debug("Service $serviceId not available: ${error.message}")
                Mono.error(error)
            }
    }

    private fun mergeAllApiDocs(
        serviceDocs: Map<String, ObjectNode>
    ): ObjectNode {
        val baseDoc = serviceDocs.values.firstOrNull() ?: return objectMapper.createObjectNode()
        
        val result = baseDoc.deepCopy() as ObjectNode
        val allPaths = objectMapper.createObjectNode()
        val allComponents = (baseDoc.get("components")?.deepCopy() as? ObjectNode) ?: objectMapper.createObjectNode()
        val allTags = objectMapper.createArrayNode()

        logger.info("📦 Starting to merge ${serviceDocs.size} services")

        val tagsSet = mutableSetOf<String>()
        
        serviceDocs.forEach { (serviceId, docs) ->
            val tags = docs.get("tags")
            if (tags != null && tags.isArray) {
                tags.forEach { tag ->
                    val tagName = tag.get("name")?.asText()
                    if (tagName != null && !tagsSet.contains(tagName)) {
                        tagsSet.add(tagName)
                        allTags.add(tag)
                    }
                }
            }

            val paths = docs.get("paths") as? ObjectNode
            val components = docs.get("components") as? ObjectNode
            
            if (paths != null && paths.size() > 0) {
                logger.info("  📍 $serviceId: ${paths.size()} paths")
                
                val pathNames = paths.fieldNames().asSequence().toList()
                pathNames.forEach { originalPath ->
                    val newPath = "/$serviceId$originalPath"
                    val pathItem = paths.get(originalPath)
                    allPaths.set<ObjectNode>(newPath, pathItem)
                    logger.debug("    → $originalPath → $newPath")
                }
            }
            
            if (components != null) {
                val schemas = components.get("schemas") as? ObjectNode
                if (schemas != null && schemas.size() > 0) {
                    logger.info("  📚 $serviceId: ${schemas.size()} schemas")
                    
                    val allSchemasNode = allComponents.get("schemas")
                        ?.let { it as ObjectNode }
                        ?: objectMapper.createObjectNode()
                    
                    val schemaNames = schemas.fieldNames().asSequence().toList()
                    schemaNames.forEach { schemaName ->
                        val schema = schemas.get(schemaName)
                        allSchemasNode.set<ObjectNode>(schemaName, schema)
                    }
                    
                    allComponents.set<ObjectNode>("schemas", allSchemasNode)
                }
                
                val securitySchemes = components.get("securitySchemes") as? ObjectNode
                if (securitySchemes != null && securitySchemes.size() > 0) {
                    logger.info("  🔐 $serviceId: ${securitySchemes.size()} security schemes")
                    
                    val allSecuritySchemesNode = allComponents.get("securitySchemes")
                        ?.let { it as ObjectNode }
                        ?: objectMapper.createObjectNode()
                    
                    val securitySchemeNames = securitySchemes.fieldNames().asSequence().toList()
                    securitySchemeNames.forEach { schemeName ->
                        val scheme = securitySchemes.get(schemeName)
                        allSecuritySchemesNode.set<ObjectNode>(schemeName, scheme)
                    }
                    
                    allComponents.set<ObjectNode>("securitySchemes", allSecuritySchemesNode)
                }
                
                val otherComponentTypes = components.fieldNames()
                    .asSequence()
                    .filter { it !in listOf("schemas", "securitySchemes") }
                    .toList()
                
                otherComponentTypes.forEach { componentType ->
                    val component = components.get(componentType) as? ObjectNode
                    if (component != null && component.size() > 0) {
                        val allComponentNode = allComponents.get(componentType)
                            ?.let { it as ObjectNode }
                            ?: objectMapper.createObjectNode()
                        
                        val itemNames = component.fieldNames().asSequence().toList()
                        itemNames.forEach { itemName ->
                            val item = component.get(itemName)
                            allComponentNode.set<ObjectNode>(itemName, item)
                        }
                        
                        allComponents.set<ObjectNode>(componentType, allComponentNode)
                    }
                }
            }
        }

        logger.info("✨ Merge result: ${allPaths.size()} total paths, ${(allComponents.get("schemas") as? ObjectNode)?.size() ?: 0} total schemas")

        result.set<ObjectNode>("paths", allPaths)
        result.set<ObjectNode>("components", allComponents)

        if (allTags.size() > 0) {
            result.set<ArrayNode>("tags", allTags)
        }

        val serversArray = objectMapper.createArrayNode()
        val serverNode = objectMapper.createObjectNode()
        serverNode.put("url", "http://localhost:8080")
        serverNode.put("description", "Local gateway")
        serversArray.add(serverNode)
        result.set<ArrayNode>("servers", serversArray)

        // Add JWT Bearer security scheme
        val securitySchemesNode = allComponents.get("securitySchemes")
            ?.let { it as ObjectNode }
            ?: objectMapper.createObjectNode()

        val bearerScheme = objectMapper.createObjectNode()
        bearerScheme.put("type", "http")
        bearerScheme.put("scheme", "bearer")
        bearerScheme.put("bearerFormat", "JWT")
        bearerScheme.put("description", "Enter your JWT token")
        securitySchemesNode.set<ObjectNode>("bearerAuth", bearerScheme)
        allComponents.set<ObjectNode>("securitySchemes", securitySchemesNode)

        // Add global security requirement
        val securityArray = objectMapper.createArrayNode()
        val securityRequirement = objectMapper.createObjectNode()
        val bearerAuthArray = objectMapper.createArrayNode()
        securityRequirement.set<ArrayNode>("bearerAuth", bearerAuthArray)
        securityArray.add(securityRequirement)
        result.set<ArrayNode>("security", securityArray)

        return result
    }
}
