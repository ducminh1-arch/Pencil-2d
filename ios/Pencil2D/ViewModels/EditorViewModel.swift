import SwiftUI
import Combine
import PencilKit

@MainActor
public final class EditorViewModel: ObservableObject {
    @Published public var project: AnimationProject
    @Published public var currentFrameIndex: Int = 0
    @Published public var currentLayerIndex: Int = 1 // Default to Layer 1 (above background)

    // Drawing Tools
    @Published public var selectedTool: DrawingTool = .pen
    @Published public var strokeColor: UIColor = .black
    @Published public var strokeWidth: CGFloat = 8.0
    @Published public var strokeOpacity: Float = 1.0

    // Onion Skinning
    @Published public var onionSkinConfig: OnionSkinConfig = OnionSkinConfig()

    // Playback
    @Published public var isPlaying: Bool = false
    @Published public var playbackFrameIndex: Int = 0

    // Exporting
    @Published public var isExporting: Bool = false
    @Published public var exportMessage: String? = nil
    @Published public var exportedFileURL: URL? = nil

    private var playbackTimer: Timer?
    private let store = ProjectStore.shared
    private var layerImages: [String: UIImage] = [:] // key: "\(frameId)_\(layerId)"

    public init(project: AnimationProject) {
        self.project = project
        self.currentFrameIndex = 0
        if let firstFrame = project.frames.first {
            self.currentLayerIndex = max(0, firstFrame.layers.count - 1)
        }
        preloadLayerImages()
    }

    private func preloadLayerImages() {
        for frame in project.frames {
            for layer in frame.layers {
                let key = "\(frame.id)_\(layer.id)"
                if let img = store.getLayerImage(projectId: project.id, frameId: frame.id, layerId: layer.id) {
                    layerImages[key] = img
                }
            }
        }
    }

    public func getLayerImage(frameId: String, layerId: String) -> UIImage? {
        let key = "\(frameId)_\(layerId)"
        return layerImages[key]
    }

    public func setLayerImage(frameId: String, layerId: String, image: UIImage) {
        let key = "\(frameId)_\(layerId)"
        layerImages[key] = image
        store.saveLayerImage(projectId: project.id, frameId: frameId, layerId: layerId, image: image)
        store.saveProject(project)
    }

    public func getCompositeImage(for frameIndex: Int) -> UIImage? {
        guard frameIndex >= 0, frameIndex < project.frames.count else { return nil }
        let frame = project.frames[frameIndex]
        var imagesForLayers = [String: UIImage]()
        for layer in frame.layers {
            let key = "\(frame.id)_\(layer.id)"
            if let img = layerImages[key] {
                imagesForLayers[layer.id] = img
            }
        }
        return FrameCompositor.compositeLayers(
            layers: frame.layers,
            layerImages: imagesForLayers,
            size: project.canvasRatio.targetSize
        )
    }

    public func getPreviousFrameComposite() -> UIImage? {
        let idx = currentFrameIndex - 1
        return idx >= 0 ? getCompositeImage(for: idx) : nil
    }

    public func getNextFrameComposite() -> UIImage? {
        let idx = currentFrameIndex + 1
        return idx < project.frames.count ? getCompositeImage(for: idx) : nil
    }

    // ==================== Timeline Management ====================

    public func selectFrame(_ index: Int) {
        guard index >= 0, index < project.frames.count else { return }
        currentFrameIndex = index
    }

    public func addBlankFrame() {
        let insertIndex = currentFrameIndex + 1
        var newFrame = AnimationFrame(index: insertIndex)

        // Duplicate layer hierarchy structure
        if let currentFrame = project.frames[safe: currentFrameIndex] {
            newFrame.layers = currentFrame.layers.map {
                DrawingLayer(name: $0.name, isVisible: true, isLocked: false, opacity: $0.opacity)
            }
        } else {
            newFrame = AnimationFrame.createDefault(index: insertIndex)
        }

        project.frames.insert(newFrame, at: insertIndex)
        reindexFrames()
        currentFrameIndex = insertIndex
        store.saveProject(project)
    }

    public func duplicateCurrentFrame() {
        guard let curFrame = project.frames[safe: currentFrameIndex] else { return }
        let insertIndex = currentFrameIndex + 1
        let newFrame = curFrame.deepCopy()

        // Clone layer images
        for layer in curFrame.layers {
            let origKey = "\(curFrame.id)_\(layer.id)"
            if let origImg = layerImages[origKey] {
                let newKey = "\(newFrame.id)_\(layer.id)"
                layerImages[newKey] = origImg
                store.saveLayerImage(projectId: project.id, frameId: newFrame.id, layerId: layer.id, image: origImg)
            }
        }

        project.frames.insert(newFrame, at: insertIndex)
        reindexFrames()
        currentFrameIndex = insertIndex
        store.saveProject(project)
    }

    public func deleteCurrentFrame() {
        guard project.frames.count > 1 else { return }
        project.frames.remove(at: currentFrameIndex)
        reindexFrames()
        currentFrameIndex = min(currentFrameIndex, project.frames.count - 1)
        store.saveProject(project)
    }

    private func reindexFrames() {
        for i in 0..<project.frames.count {
            project.frames[i].index = i
        }
    }

    // ==================== Layer Management ====================

    public func selectLayer(_ index: Int) {
        guard let curFrame = project.frames[safe: currentFrameIndex],
              index >= 0, index < curFrame.layers.count else { return }
        currentLayerIndex = index
    }

    public func addLayer(name: String) {
        for i in 0..<project.frames.count {
            let newLayer = DrawingLayer(name: name.isEmpty ? "Layer \(project.frames[i].layers.count + 1)" : name)
            project.frames[i].layers.append(newLayer)
        }
        if let curFrame = project.frames[safe: currentFrameIndex] {
            currentLayerIndex = curFrame.layers.count - 1
        }
        store.saveProject(project)
    }

    public func toggleLayerVisibility(at index: Int) {
        guard let curFrame = project.frames[safe: currentFrameIndex],
              index >= 0, index < curFrame.layers.count else { return }
        project.frames[currentFrameIndex].layers[index].isVisible.toggle()
        store.saveProject(project)
    }

    public func toggleLayerLock(at index: Int) {
        guard let curFrame = project.frames[safe: currentFrameIndex],
              index >= 0, index < curFrame.layers.count else { return }
        project.frames[currentFrameIndex].layers[index].isLocked.toggle()
        store.saveProject(project)
    }

    public func setLayerOpacity(at index: Int, opacity: Float) {
        guard let curFrame = project.frames[safe: currentFrameIndex],
              index >= 0, index < curFrame.layers.count else { return }
        project.frames[currentFrameIndex].layers[index].opacity = opacity
        store.saveProject(project)
    }

    // ==================== Onion Skinning ====================

    public func toggleOnionSkin() {
        onionSkinConfig.isEnabled.toggle()
    }

    // ==================== Playback ====================

    public func togglePlayback() {
        if isPlaying {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    public func startPlayback() {
        guard !project.frames.isEmpty else { return }
        isPlaying = true
        playbackFrameIndex = currentFrameIndex

        playbackTimer?.invalidate()
        let interval = 1.0 / Double(max(1, project.fps))
        playbackTimer = Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            let next = self.playbackFrameIndex + 1
            if next >= self.project.frames.count {
                if self.project.loopPlayback {
                    self.playbackFrameIndex = 0
                } else {
                    self.stopPlayback()
                }
            } else {
                self.playbackFrameIndex = next
            }
        }
    }

    public func stopPlayback() {
        isPlaying = false
        playbackTimer?.invalidate()
        playbackTimer = nil
    }

    public func setCanvasRatio(_ ratio: CanvasRatio) {
        guard project.canvasRatio != ratio else { return }
        project.canvasRatio = ratio
        store.saveProject(project)
    }

    public func setFps(_ fps: Int) {
        project.fps = fps
        if isPlaying {
            startPlayback()
        }
        store.saveProject(project)
    }

    // ==================== Export ====================

    public func exportAnimation(option: ExportOption, completion: @escaping (URL?) -> Void) {
        isExporting = true
        exportMessage = "Rendering animation frames..."

        var frames = [UIImage]()
        for i in 0..<project.frames.count {
            if let img = getCompositeImage(for: i) {
                frames.append(img)
            }
        }

        guard !frames.isEmpty else {
            isExporting = false
            completion(nil)
            return
        }

        let tempDir = FileManager.default.temporaryDirectory
        let filename = "Pencil2D_\(project.title.replacingOccurrences(of: " ", with: "_"))_\(Int(Date().timeIntervalSince1970)).\(option.format.rawValue)"
        let outputURL = tempDir.appendingPathComponent(filename)

        switch option.format {
        case .mp4:
            exportMessage = "Encoding MP4 video..."
            VideoExporter.exportToMP4(images: frames, fps: option.fps, outputURL: outputURL, loopCount: option.loopCount) { [weak self] result in
                guard let self = self else { return }
                self.isExporting = false
                switch result {
                case .success(let url):
                    self.exportedFileURL = url
                    completion(url)
                case .failure:
                    completion(nil)
                }
            }
        case .gif:
            exportMessage = "Generating GIF..."
            DispatchQueue.global(qos: .userInitiated).async { [weak self] in
                let success = GIFExporter.exportToGIF(images: frames, fps: option.fps, outputURL: outputURL, loopCount: 0)
                DispatchQueue.main.async {
                    self?.isExporting = false
                    if success {
                        self?.exportedFileURL = outputURL
                        completion(outputURL)
                    } else {
                        completion(nil)
                    }
                }
            }
        case .pngSequence:
            exportMessage = "Exporting PNG..."
            if let firstFrame = frames.first, let data = firstFrame.pngData() {
                try? data.write(to: outputURL)
                isExporting = false
                exportedFileURL = outputURL
                completion(outputURL)
            } else {
                isExporting = false
                completion(nil)
            }
        }
    }
}

extension Array {
    subscript(safe index: Index) -> Element? {
        return indices.contains(index) ? self[index] : nil
    }
}
