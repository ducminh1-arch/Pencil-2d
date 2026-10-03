import UIKit
import ImageIO
import UniformTypeIdentifiers

public final class GIFExporter {

    public static func exportToGIF(
        images: [UIImage],
        fps: Int,
        outputURL: URL,
        loopCount: Int = 0 // 0 = infinite loop
    ) -> Bool {
        guard !images.isEmpty else { return false }

        let frameDelay = 1.0 / Double(max(1, fps))
        let fileProperties: [CFString: Any] = [
            kCGImagePropertyGIFDictionary: [
                kCGImagePropertyGIFLoopCount: loopCount
            ]
        ]

        let frameProperties: [CFString: Any] = [
            kCGImagePropertyGIFDictionary: [
                kCGImagePropertyGIFDelayTime: frameDelay
            ]
        ]

        guard let destination = CGImageDestinationCreateWithURL(
            outputURL as CFURL,
            UTType.gif.identifier as CFString,
            images.count,
            nil
        ) else { return false }

        CGImageDestinationSetProperties(destination, fileProperties as CFDictionary)

        for image in images {
            if let cgImage = image.cgImage {
                CGImageDestinationAddImage(destination, cgImage, frameProperties as CFDictionary)
            }
        }

        return CGImageDestinationFinalize(destination)
    }
}
