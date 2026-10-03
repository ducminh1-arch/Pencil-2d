import Foundation
import CoreGraphics

public enum CanvasRatio: String, CaseIterable, Identifiable, Codable {
    case ratio1_1 = "1:1"
    case ratio16_9 = "16:9"
    case ratio4_3 = "4:3"
    case ratio9_16 = "9:16"

    public var id: String { rawValue }

    public var label: String { rawValue }

    public var description: String {
        switch self {
        case .ratio1_1: return "Square (Instagram/Social)"
        case .ratio16_9: return "Landscape (YouTube/Cinema)"
        case .ratio4_3: return "Classic (Tablet/TV)"
        case .ratio9_16: return "Portrait (TikTok/Reels)"
        }
    }

    public var targetSize: CGSize {
        switch self {
        case .ratio1_1: return CGSize(width: 1080, height: 1080)
        case .ratio16_9: return CGSize(width: 1920, height: 1080)
        case .ratio4_3: return CGSize(width: 1440, height: 1080)
        case .ratio9_16: return CGSize(width: 1080, height: 1920)
        }
    }

    public var aspectRatioMultiplier: CGFloat {
        let size = targetSize
        return size.width / size.height
    }
}
