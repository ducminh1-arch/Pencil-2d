import Foundation

public struct DrawingLayer: Identifiable, Codable, Equatable {
    public var id: String
    public var name: String
    public var isVisible: BooleanLiteralType
    public var isLocked: Bool
    public var opacity: Float
    public var drawingData: Data?

    public init(
        id: String = UUID().uuidString,
        name: String,
        isVisible: Bool = true,
        isLocked: Bool = false,
        opacity: Float = 1.0,
        drawingData: Data? = nil
    ) {
        self.id = id
        self.name = name
        self.isVisible = isVisible
        self.isLocked = isLocked
        self.opacity = opacity
        self.drawingData = drawingData
    }

    public func deepCopy() -> DrawingLayer {
        return DrawingLayer(
            id: UUID().uuidString,
            name: name,
            isVisible: isVisible,
            isLocked: isLocked,
            opacity: opacity,
            drawingData: drawingData
        )
    }
}
