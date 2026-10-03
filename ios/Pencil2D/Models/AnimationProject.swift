import Foundation

public struct AnimationProject: Identifiable, Codable, Equatable {
    public var id: String
    public var title: String
    public var canvasRatio: CanvasRatio
    public var fps: Int
    public var projectType: String
    public var createdAt: Date
    public var modifiedAt: Date
    public var frames: [AnimationFrame]
    public var thumbnailData: Data?

    public init(
        id: String = UUID().uuidString,
        title: String = "My Animation",
        canvasRatio: CanvasRatio = .ratio1_1,
        fps: Int = 12,
        loopPlayback: Bool = true,
        projectType: String = "animation",
        createdAt: Date = Date(),
        modifiedAt: Date = Date(),
        frames: [AnimationFrame] = [],
        thumbnailData: Data? = nil
    ) {
        self.id = id
        self.title = title
        self.canvasRatio = canvasRatio
        self.fps = fps
        self.loopPlayback = loopPlayback
        self.projectType = projectType
        self.createdAt = createdAt
        self.modifiedAt = modifiedAt
        self.frames = frames
        self.thumbnailData = thumbnailData
    }

    public var isPhotoProject: Bool {
        projectType == "image"
    }

    public var frameCount: Int {
        frames.count
    }

    public var durationSeconds: Double {
        guard fps > 0 else { return 0 }
        return Double(frameCount) / Double(fps)
    }
}
