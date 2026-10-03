import UIKit

public final class FrameCompositor {

    public static func compositeLayers(
        layers: [DrawingLayer],
        layerImages: [String: UIImage],
        size: CGSize,
        backgroundColor: UIColor = .white
    ) -> UIImage {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1.0
        let renderer = UIGraphicsImageRenderer(size: size, format: format)

        return renderer.image { context in
            backgroundColor.setFill()
            context.fill(CGRect(origin: .zero, size: size))

            for layer in layers {
                guard layer.isVisible, let image = layerImages[layer.id] else { continue }
                image.draw(in: CGRect(origin: .zero, size: size), blendMode: .normal, alpha: CGFloat(layer.opacity))
            }
        }
    }

    public static func renderOnionSkin(
        image: UIImage,
        tintColor: UIColor,
        alpha: CGFloat,
        size: CGSize
    ) -> UIImage {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1.0
        let renderer = UIGraphicsImageRenderer(size: size, format: format)

        return renderer.image { context in
            let rect = CGRect(origin: .zero, size: size)

            // Draw image with alpha
            image.draw(in: rect, blendMode: .normal, alpha: alpha)

            // Tint overlay
            context.cgContext.setBlendMode(.sourceAtop)
            tintColor.withAlphaComponent(alpha).setFill()
            context.fill(rect)
        }
    }
}
