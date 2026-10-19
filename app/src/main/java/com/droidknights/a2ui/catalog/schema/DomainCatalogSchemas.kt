package com.droidknights.a2ui.catalog.schema

/**
 * Facade grouping the 5 individual component schema contracts located in `catalog/schema/`:
 * - [CircularClimateDialSchema]
 * - [PowerBudgetBarSchema]
 * - [HotspotDiagramCardSchema]
 * - [ActionGuardContainerSchema]
 * - [MetricSparklineSchema]
 */
object DomainCatalogSchemas {
    typealias CircularClimateDial = CircularClimateDialSchema
    typealias PowerBudgetBar = PowerBudgetBarSchema
    typealias HotspotDiagramCard = HotspotDiagramCardSchema
    typealias ActionGuardContainer = ActionGuardContainerSchema
    typealias MetricSparkline = MetricSparklineSchema
    typealias BidirectionalExchangeInput = BidirectionalExchangeInputSchema
}
