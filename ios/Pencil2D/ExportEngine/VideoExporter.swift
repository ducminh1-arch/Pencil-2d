import UIKit
import AVFoundation

public final class VideoExporter {

    public static func exportToMP4(
        images: [UIImage],
        fps: Int,
        outputURL: URL,
        loopCount: Int = 1,
        completion: @escaping (Result<URL, Error>) -> Void
    ) {
        guard let firstImage = images.first else {
            completion(.failure(NSError(domain: "VideoExporter", code: -1, userInfo: [NSLocalizedDescriptionKey: "No frames to export"])))
            return
        }

        var width = Int(firstImage.size.width)
        var height = Int(firstImage.size.height)
        if width % 2 != 0 { width -= 1 }
        if height % 2 != 0 { height -= 1 }

        if FileManager.default.fileExists(atPath: outputURL.path) {
            try? FileManager.default.removeItem(at: outputURL)
        }

        do {
            let assetWriter = try AVAssetWriter(outputURL: outputURL, fileType: .mp4)

            let videoSettings: [String: Any] = [
                AVVideoCodecKey: AVVideoCodecType.h264,
                AVVideoWidthKey: width,
                AVVideoHeightKey: height,
                AVVideoCompressionPropertiesKey: [
                    AVVideoAverageBitRateKey: 4_000_000,
                    AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel
                ]
            ]

            let writerInput = AVAssetWriterInput(mediaType: .video, outputSettings: videoSettings)
            writerInput.expectsMediaDataInRealTime = false

            let pixelBufferAttributes: [String: Any] = [
                kCVPixelBufferPixelFormatTypeKey as String: Int(kCVPixelFormatType_32ARGB),
                kCVPixelBufferWidthKey as String: width,
                kCVPixelBufferHeightKey as String: height
            ]

            let adaptor = AVAssetWriterInputPixelBufferAdaptor(
                assetWriterInput: writerInput,
                sourcePixelBufferAttributes: pixelBufferAttributes
            )

            guard assetWriter.canAdd(writerInput) else {
                completion(.failure(NSError(domain: "VideoExporter", code: -2, userInfo: [NSLocalizedDescriptionKey: "Cannot add writer input"])))
                return
            }

            assetWriter.add(writerInput)
            assetWriter.startWriting()
            assetWriter.startSession(atSourceTime: .zero)

            var frameCount: Int64 = 0
            let frameDuration = CMTime(value: 1, timescale: CMTimeScale(fps))

            let queue = DispatchQueue(label: "videoExportQueue")
            writerInput.requestMediaDataWhenReady(on: queue) {
                var allImages = [UIImage]()
                for _ in 0..<max(1, loopCount) {
                    allImages.append(contentsOf: images)
                }

                for image in allImages {
                    while !writerInput.isReadyForMoreMediaData {
                        Thread.sleep(forTimeInterval: 0.01)
                    }

                    if let pixelBuffer = self.pixelBuffer(from: image, width: width, height: height) {
                        let presentTime = CMTimeMultiply(frameDuration, multiplier: Int32(frameCount))
                        adaptor.append(pixelBuffer, withPresentationTime: presentTime)
                        frameCount += 1
                    }
                }

                writerInput.markAsFinished()
                assetWriter.finishWriting {
                    DispatchQueue.main.async {
                        if assetWriter.status == .completed {
                            completion(.success(outputURL))
                        } else {
                            completion(.failure(assetWriter.error ?? NSError(domain: "VideoExporter", code: -3, userInfo: [NSLocalizedDescriptionKey: "Export failed"])))
                        }
                    }
                }
            }
        } catch {
            completion(.failure(error))
        }
    }

    private static func pixelBuffer(from image: UIImage, width: Int, height: Int) -> CVPixelBuffer? {
        var pixelBuffer: CVPixelBuffer?
        let attrs: [String: Any] = [
            kCVPixelBufferCGImageCompatibilityKey as String: true,
            kCVPixelBufferCGBitmapContextCompatibilityKey as String: true
        ]

        let status = CVPixelBufferCreate(
            kCFAllocatorDefault,
            width,
            height,
            kCVPixelFormatType_32ARGB,
            attrs as CFDictionary,
            &pixelBuffer
        )

        guard status == kCVReturnSuccess, let buffer = pixelBuffer else { return nil }

        CVPixelBufferLockBaseAddress(buffer, [])
        defer { CVPixelBufferUnlockBaseAddress(buffer, []) }

        let context = CGContext(
            data: CVPixelBufferGetBaseAddress(buffer),
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: CVPixelBufferGetBytesPerRow(buffer),
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue
        )

        if let cgImage = image.cgImage {
            context?.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
        }

        return buffer
    }
}
