import Foundation

public struct OnionSkinConfig: Codable, Equatable {
    public var isEnabled: Bool = true
    public var prevFramesCount: Int = 1
    public var nextFramesCount: Int = 1
    public var prevFrameTintColorHex: String = "#E63232" // Red tint
    public var nextFrameTintColorHex: String = "#32B432" // Green tint
    public var opacity: Float = 0.35

    public init(
        isEnabled: Bool = true,
        prevFramesCount: Int = 1,
        nextFramesCount: Int = 1,
        prevFrameTintColorHex: String = "#E63232",
        nextFrameTintColorHex: String = "#32B432",
        opacity: Float = 0.35
    ) {
        self.isEnabled = isEnabled
        self.prevFramesCount = prevFramesCount
        self.nextFramesCount = nextFramesCount
        self.prevFrameTintColorHex = prevFrameTintColorHex
        self.nextFrameTintColorHex = nextFrameTintColorHex
        self.opacity = opacity
    }
}

public enum ExportFormat: String, CaseIterable, Identifiable, Codable {
    case mp4 = "mp4"
    case gif = "gif"
    case pngSequence = "png_sequence"

    public var id: String { rawValue }

    public var label: String {
        switch self {
        case .mp4: return "MP4 Video"
        case .gif: return "Animated GIF"
        case .pngSequence: return "PNG Sequence"
        }
    }

    public var utType: String {
        switch self {
        case .mp4: return "public.mpeg-4"
        case .gif: return "com.compuserve.gif"
        case .pngSequence: return "public.zip-archive"
        }
    }
}

public struct ExportOption {
    public var format: ExportFormat
    public var fps: Int
    public var loopCount: Int

    public init(
        format: ExportFormat = .mp4,
        fps: Int = 12,
        loopCount: Int = 1
    ) {
        self.format = format
        self.fps = fps
        self.loopCount = loopCount
    }
}
