import SwiftUI
import PencilKit

public struct EditorView: View {
    @StateObject private var viewModel: EditorViewModel
    @State private var canvasView = PKCanvasView()
    @State private var activeTab: String = "brush" // "brush", "eraser", "canvas", "settings"
    @State private var showExportSheet = false

    public var onDismiss: () -> Void

    public init(project: AnimationProject, onDismiss: @escaping () -> Void) {
        self._viewModel = StateObject(wrappedValue: EditorViewModel(project: project))
        self.onDismiss = onDismiss
    }

    public var body: some View {
        VStack(spacing: 0) {
            // ==================== Top Bar (matching Phone 1-4) ====================
            HStack {
                Button(action: onDismiss) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(Color(red: 0.12, green: 0.12, blue: 0.14))
                }

                Spacer()

                // Center Two-Pill Switcher (Onion Skin + Undo)
                HStack(spacing: 4) {
                    Button(action: { viewModel.toggleOnionSkin() }) {
                        Image(systemName: "circle.circle")
                            .font(.system(size: 16))
                            .foregroundColor(viewModel.onionSkinConfig.isEnabled ? .black : .gray)
                            .frame(width: 32, height: 28)
                    }

                    Rectangle()
                        .fill(Color(white: 0.85))
                        .frame(width: 1, height: 16)

                    Button(action: {
                        canvasView.undoManager?.undo()
                        saveCanvas()
                    }) {
                        Image(systemName: "arrow.uturn.backward")
                            .font(.system(size: 14))
                            .foregroundColor(.gray)
                            .frame(width: 32, height: 28)
                    }
                }
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(Color.white)
                .clipShape(Capsule())
                .shadow(color: Color.black.opacity(0.08), radius: 3, x: 0, y: 1)

                Spacer()

                Button("Next") {
                    showExportSheet = true
                }
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(Color(red: 0.12, green: 0.12, blue: 0.14))
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 6)

            // ==================== Center Canvas ====================
            ZStack {
                Color(red: 0.94, green: 0.94, blue: 0.95).ignoresSafeArea()

                let ratio = viewModel.project.canvasRatio.aspectRatioMultiplier

                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(Color.white)
                        .shadow(color: Color.black.opacity(0.08), radius: 6, x: 0, y: 3)

                    // 1. Onion Skin Previous Frame (faint silhouette)
                    if viewModel.onionSkinConfig.isEnabled,
                       let prevImage = viewModel.getPreviousFrameComposite() {
                        Image(uiImage: prevImage)
                            .resizable()
                            .scaledToFit()
                            .opacity(0.3)
                            .allowsHitTesting(false)
                    }

                    // 2. Active Drawing Layer with PencilKit
                    PencilKitCanvasView(
                        canvasView: $canvasView,
                        selectedTool: viewModel.selectedTool,
                        color: viewModel.strokeColor,
                        strokeWidth: viewModel.strokeWidth,
                        onDrawingChanged: {
                            saveCanvas()
                        }
                    )
                    .cornerRadius(14)

                    // 3. Center Translucent Circular Play Button (Phone 4: ENJOY YOUR CREATIVITY)
                    Button(action: { viewModel.togglePlayback() }) {
                        Circle()
                            .fill(Color.white.opacity(0.85))
                            .frame(width: 60, height: 60)
                            .overlay(
                                Image(systemName: viewModel.isPlaying ? "pause.fill" : "play.fill")
                                    .font(.system(size: 24))
                                    .foregroundColor(Color(red: 0.2, green: 0.2, blue: 0.2))
                            )
                            .shadow(color: Color.black.opacity(0.15), radius: 6, x: 0, y: 3)
                    }
                }
                .aspectRatio(ratio, contentMode: .fit)
                .padding(12)
            }

            // ==================== Timeline Scrubber (matching Phone 1) ====================
            HStack(spacing: 10) {
                // Square Play Button on Left
                Button(action: { viewModel.togglePlayback() }) {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color(red: 0.90, green: 0.90, blue: 0.92))
                        .frame(width: 48, height: 48)
                        .overlay(
                            Image(systemName: viewModel.isPlaying ? "pause.fill" : "play.fill")
                                .font(.system(size: 20))
                                .foregroundColor(Color(white: 0.35))
                        )
                }

                // Horizontal Frame List
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Array(viewModel.project.frames.enumerated()), id: \.element.id) { index, frame in
                            let isSelected = index == viewModel.currentFrameIndex

                            Button(action: {
                                saveCanvas()
                                viewModel.selectFrame(index)
                                loadCanvas()
                            }) {
                                VStack(spacing: 2) {
                                    ZStack {
                                        RoundedRectangle(cornerRadius: 8)
                                            .fill(Color.white)
                                            .frame(width: 44, height: 44)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 8)
                                                    .stroke(isSelected ? Color(red: 1.0, green: 0.18, blue: 0.33) : Color(white: 0.8), lineWidth: isSelected ? 2.5 : 1)
                                            )

                                        if let thumb = viewModel.getCompositeImage(for: index) {
                                            Image(uiImage: thumb)
                                                .resizable()
                                                .scaledToFit()
                                                .frame(width: 40, height: 40)
                                                .cornerRadius(6)
                                        }
                                    }

                                    Text("\(index + 1)")
                                        .font(.system(size: 10, weight: .bold))
                                        .foregroundColor(isSelected ? Color(red: 1.0, green: 0.18, blue: 0.33) : Color(white: 0.55))
                                }
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.vertical, 2)
                }

                // Add Frame (+)
                Button(action: {
                    saveCanvas()
                    viewModel.addBlankFrame()
                    loadCanvas()
                }) {
                    RoundedRectangle(cornerRadius: 8)
                        .strokeBorder(Color(white: 0.7), style: StrokeStyle(lineWidth: 1, dash: [4]))
                        .frame(width: 44, height: 44)
                        .overlay(
                            Image(systemName: "plus")
                                .font(.system(size: 18))
                                .foregroundColor(.gray)
                        )
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(Color.white)
            .border(Color(white: 0.92), width: 1)

            // ==================== Bottom Sheet (Brush / Canvas / Settings) ====================
            if activeTab == "brush" {
                VStack(spacing: 8) {
                    Text("Brush")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(.black)

                    HStack(spacing: 12) {
                        Text("\(Int(viewModel.strokeWidth))")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.gray)
                            .frame(width: 20)

                        Slider(value: $viewModel.strokeWidth, in: 2...50)
                            .tint(Color(red: 0.30, green: 0.85, blue: 0.39))
                    }
                    .padding(.horizontal, 12)

                    HStack {
                        // Rainbow picker
                        Circle()
                            .fill(Color(white: 0.9))
                            .frame(width: 28, height: 28)
                            .overlay(Image(systemName: "paintpalette").font(.system(size: 13)).foregroundColor(.gray))

                        Spacer()

                        let colors: [UIColor] = [
                            .black,
                            UIColor(red: 0.31, green: 0.46, blue: 1.0, alpha: 1.0),
                            UIColor(red: 0.49, green: 0.30, blue: 1.0, alpha: 1.0),
                            UIColor(red: 0.85, green: 0.11, blue: 0.38, alpha: 1.0),
                            UIColor(red: 0.15, green: 0.65, blue: 0.60, alpha: 1.0),
                            UIColor(red: 0.98, green: 0.55, blue: 0.0, alpha: 1.0),
                            UIColor(red: 0.75, green: 0.79, blue: 0.20, alpha: 1.0)
                        ]

                        ForEach(colors, id: \.self) { c in
                            Circle()
                                .fill(Color(c))
                                .frame(width: 28, height: 28)
                                .overlay(
                                    Circle().stroke(c == viewModel.strokeColor ? Color(red: 1.0, green: 0.18, blue: 0.33) : Color.clear, lineWidth: 3)
                                )
                                .onTapGesture {
                                    viewModel.strokeColor = c
                                    viewModel.selectedTool = .pen
                                }
                        }
                    }
                    .padding(.horizontal, 10)
                }
                .padding(.vertical, 10)
                .background(Color.white)
            } else if activeTab == "canvas" {
                VStack(spacing: 8) {
                    Text("Canvas Ratio")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(Color(red: 0.12, green: 0.12, blue: 0.14))

                    HStack(spacing: 12) {
                        ForEach(CanvasRatio.allCases) { r in
                            let isSel = viewModel.project.canvasRatio == r
                            Button(action: { viewModel.setCanvasRatio(r) }) {
                                VStack(spacing: 6) {
                                    RoundedRectangle(cornerRadius: 4)
                                        .stroke(isSel ? Color(red: 1.0, green: 0.18, blue: 0.33) : Color.gray, lineWidth: 1.5)
                                        .background(isSel ? Color(red: 1.0, green: 0.18, blue: 0.33).opacity(0.15) : Color(white: 0.95))
                                        .frame(
                                            width: r == .ratio16_9 ? 34 : (r == .ratio4_3 ? 30 : (r == .ratio9_16 ? 18 : 24)),
                                            height: r == .ratio9_16 ? 34 : (r == .ratio16_9 ? 18 : (r == .ratio4_3 ? 22 : 24))
                                        )

                                    Text(r.label)
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(isSel ? Color(red: 1.0, green: 0.18, blue: 0.33) : Color(red: 0.2, green: 0.2, blue: 0.2))
                                }
                                .padding(.horizontal, 10)
                                .padding(.vertical, 8)
                                .background(isSel ? Color(red: 1.0, green: 0.93, blue: 0.95) : Color(white: 0.97))
                                .cornerRadius(10)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(isSel ? Color(red: 1.0, green: 0.18, blue: 0.33) : Color(white: 0.88), lineWidth: isSel ? 2 : 1)
                                )
                            }
                        }
                    }
                    .padding(.vertical, 4)
                }
                .padding(.vertical, 10)
                .background(Color.white)
            } else if activeTab == "settings" {
                HStack {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Animation FPS")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(Color(red: 0.12, green: 0.12, blue: 0.14))
                        HStack(spacing: 8) {
                            ForEach([6, 8, 12, 24], id: \.self) { f in
                                let isSel = viewModel.project.fps == f
                                Button("\(f)") {
                                    viewModel.setFps(f)
                                }
                                .font(.system(size: 12, weight: .bold))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(isSel ? Color(red: 1.0, green: 0.18, blue: 0.33) : Color(white: 0.93))
                                .foregroundColor(isSel ? .white : Color(red: 0.12, green: 0.12, blue: 0.14))
                                .cornerRadius(8)
                            }
                        }
                    }
                    Spacer()
                    VStack(alignment: .trailing, spacing: 6) {
                        Text("Onion Skin")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(Color(red: 0.12, green: 0.12, blue: 0.14))
                        Button(viewModel.onionSkinConfig.isEnabled ? "ON" : "OFF") {
                            viewModel.toggleOnionSkin()
                        }
                        .font(.system(size: 12, weight: .heavy))
                        .padding(.horizontal, 14)
                        .padding(.vertical, 6)
                        .background(viewModel.onionSkinConfig.isEnabled ? Color.green.opacity(0.2) : Color(white: 0.93))
                        .foregroundColor(viewModel.onionSkinConfig.isEnabled ? .green : .gray)
                        .cornerRadius(10)
                    }
                }
                .padding(14)
                .background(Color.white)
            }

            // ==================== 4-Tab Bottom Dock ====================
            HStack {
                DockButton(icon: "pencil.tip", label: "Brush", isSelected: activeTab == "brush") {
                    activeTab = "brush"
                    viewModel.selectedTool = .pen
                }
                DockButton(icon: "eraser.fill", label: "Eraser", isSelected: activeTab == "eraser") {
                    activeTab = "eraser"
                    viewModel.selectedTool = .eraser
                }
                DockButton(icon: "square.dashed", label: "Canvas", isSelected: activeTab == "canvas") {
                    activeTab = "canvas"
                }
                DockButton(icon: "gearshape", label: "Settings", isSelected: activeTab == "settings") {
                    activeTab = "settings"
                }
            }
            .padding(.vertical, 6)
            .background(Color.white)
            .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: -2)
        }
        .sheet(isPresented: $showExportSheet) {
            ExportSheet(viewModel: viewModel)
        }
    }

    private func saveCanvas() {
        guard let curFrame = viewModel.project.frames[safe: viewModel.currentFrameIndex],
              let curLayer = curFrame.layers[safe: viewModel.currentLayerIndex] else { return }
        let bounds = viewModel.project.canvasRatio.targetSize
        let image = canvasView.drawing.image(from: CGRect(origin: .zero, size: bounds), scale: 1.0)
        viewModel.setLayerImage(frameId: curFrame.id, layerId: curLayer.id, image: image)
    }

    private func loadCanvas() {
        canvasView.drawing = PKDrawing()
    }
}

struct DockButton: View {
    let icon: String
    let label: String
    let isSelected: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            VStack(spacing: 2) {
                Image(systemName: icon)
                    .font(.system(size: 20))
                    .foregroundColor(isSelected ? Color(red: 0.12, green: 0.12, blue: 0.14) : Color(white: 0.6))

                Text(label)
                    .font(.system(size: 10, weight: isSelected ? .bold : .medium))
                    .foregroundColor(isSelected ? Color(red: 0.12, green: 0.12, blue: 0.14) : Color(white: 0.6))
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }
}
