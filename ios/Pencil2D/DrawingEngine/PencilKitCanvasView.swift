import SwiftUI
import PencilKit

public struct PencilKitCanvasView: UIViewRepresentable {
    @Binding public var canvasView: PKCanvasView
    public var selectedTool: DrawingTool
    public var color: UIColor
    public var strokeWidth: CGFloat
    public var onDrawingChanged: () -> Void

    public init(
        canvasView: Binding<PKCanvasView>,
        selectedTool: DrawingTool,
        color: UIColor,
        strokeWidth: CGFloat,
        onDrawingChanged: @escaping () -> Void
    ) {
        self._canvasView = canvasView
        self.selectedTool = selectedTool
        self.color = color
        self.strokeWidth = strokeWidth
        self.onDrawingChanged = onDrawingChanged
    }

    public func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    public func makeUIView(context: Context) -> PKCanvasView {
        canvasView.delegate = context.coordinator
        canvasView.drawingPolicy = .anyInput
        canvasView.backgroundColor = .clear
        canvasView.isOpaque = false
        updateTool(canvasView)
        return canvasView
    }

    public func updateUIView(_ uiView: PKCanvasView, context: Context) {
        updateTool(uiView)
    }

    private func updateTool(_ uiView: PKCanvasView) {
        switch selectedTool {
        case .pen:
            uiView.tool = PKInkingTool(.pen, color: color, width: strokeWidth)
        case .pencil:
            uiView.tool = PKInkingTool(.pencil, color: color, width: strokeWidth)
        case .eraser:
            uiView.tool = PKEraserTool(.vector)
        case .fill:
            // Fill handled separately by tap gesture
            break
        }
    }

    public class Coordinator: NSObject, PKCanvasViewDelegate {
        var parent: PencilKitCanvasView

        init(_ parent: PencilKitCanvasView) {
            self.parent = parent
        }

        public func canvasViewDrawingDidChange(_ canvasView: PKCanvasView) {
            parent.onDrawingChanged()
        }
    }
}
