import Foundation

public struct AnimationFrame: Identifiable, Codable, Equatable {
    public var id: String
    public var index: Int
    public var layers: [DrawingLayer]
    public var thumbnailData: Data?

    public init(
        id: String = UUID().uuidString,
        index: Int,
        layers: [DrawingLayer] = [],
        thumbnailData: Data? = nil
    ) {
        self.id = id
        self.index = index
        self.layers = layers
        self.thumbnailData = thumbnailData
    }

    public static func createDefault(index: Int) -> AnimationFrame {
        var frame = AnimationFrame(index: index)
        frame.layers = [
            DrawingLayer(name: "Background", isVisible: true, isLocked: false, opacity: 1.0),
            DrawingLayer(name: "Layer 1", isVisible: true, isLocked: false, opacity: 1.0)
        ]
        return frame
    }

    public func deepCopy() -> AnimationFrame {
        return AnimationFrame(
            id: UUID().uuidString,
            index: index,
            layers: layers.map { $0.deepCopy() },
            thumbnailData: thumbnailData
        )
    }
}
