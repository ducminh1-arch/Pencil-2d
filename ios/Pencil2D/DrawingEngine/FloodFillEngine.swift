import UIKit

public final class FloodFillEngine {

    public static func floodFill(
        image: UIImage,
        at startPoint: CGPoint,
        fillColor: UIColor,
        tolerance: CGFloat = 0.1
    ) -> UIImage? {
        guard let cgImage = image.cgImage else { return nil }

        let width = cgImage.width
        let height = cgImage.height
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let bytesPerPixel = 4
        let bytesPerRow = bytesPerPixel * width
        let bitsPerComponent = 8

        var rawData = [UInt8](repeating: 0, count: height * bytesPerRow)
        guard let context = CGContext(
            data: &rawData,
            width: width,
            height: height,
            bitsPerComponent: bitsPerComponent,
            bytesPerRow: bytesPerRow,
            space: colorSpace,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue
        ) else { return nil }

        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))

        let startX = Int(startPoint.x)
        let startY = Int(startPoint.y)
        guard startX >= 0, startX < width, startY >= 0, startY < height else { return nil }

        let startOffset = (startY * width + startX) * bytesPerPixel
        let targetR = rawData[startOffset]
        let targetG = rawData[startOffset + 1]
        let targetB = rawData[startOffset + 2]
        let targetA = rawData[startOffset + 3]

        var fillR: CGFloat = 0, fillG: CGFloat = 0, fillB: CGFloat = 0, fillA: CGFloat = 0
        fillColor.getRed(&fillR, green: &fillG, blue: &fillB, alpha: &fillA)
        let newR = UInt8(fillR * 255)
        let newG = UInt8(fillG * 255)
        let newB = UInt8(fillB * 255)
        let newA = UInt8(fillA * 255)

        if abs(Int(targetR) - Int(newR)) < 5 &&
           abs(Int(targetG) - Int(newG)) < 5 &&
           abs(Int(targetB) - Int(newB)) < 5 &&
           abs(Int(targetA) - Int(newA)) < 5 {
            return image
        }

        var queue = [Int]()
        queue.reserveCapacity(width * 4)
        var visited = [Bool](repeating: false, count: width * height)

        let startIdx = startY * width + startX
        queue.append(startIdx)
        visited[startIdx] = true

        let tol = Int(tolerance * 255 * 4)

        while !queue.isEmpty {
            let currIdx = queue.removeFirst()
            let cx = currIdx % width
            let cy = currIdx / width
            let offset = currIdx * bytesPerPixel

            rawData[offset] = newR
            rawData[offset + 1] = newG
            rawData[offset + 2] = newB
            rawData[offset + 3] = newA

            let neighbors = [
                (cx + 1, cy),
                (cx - 1, cy),
                (cx, cy + 1),
                (cx, cy - 1)
            ]

            for (nx, ny) in neighbors {
                if nx >= 0 && nx < width && ny >= 0 && ny < height {
                    let nIdx = ny * width + nx
                    if !visited[nIdx] {
                        let nOffset = nIdx * bytesPerPixel
                        let nr = rawData[nOffset]
                        let ng = rawData[nOffset + 1]
                        let nb = rawData[nOffset + 2]
                        let na = rawData[nOffset + 3]

                        let diff = abs(Int(nr) - Int(targetR)) +
                                   abs(Int(ng) - Int(targetG)) +
                                   abs(Int(nb) - Int(targetB)) +
                                   abs(Int(na) - Int(targetA))

                        if diff <= tol {
                            visited[nIdx] = true
                            queue.append(nIdx)
                        }
                    }
                }
            }
        }

        guard let outputCGImage = context.makeImage() else { return nil }
        return UIImage(cgImage: outputCGImage)
    }
}
