import UIKit

public final class ProjectStore {
    public static let shared = ProjectStore()

    private let fileManager = FileManager.default

    private var projectsDirectory: URL {
        let docs = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let dir = docs.appendingPathComponent("Projects", isDirectory: true)
        if !fileManager.fileExists(atPath: dir.path) {
            try? fileManager.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }

    public func getAllProjects() -> [AnimationProject] {
        guard let subdirs = try? fileManager.contentsOfDirectory(at: projectsDirectory, includingPropertiesForKeys: nil) else {
            return [createSampleStickmanProject(), createSampleBouncingBallProject()]
        }

        var projects = [AnimationProject]()
        let decoder = JSONDecoder()

        for dir in subdirs where dir.hasDirectoryPath {
            let metaURL = dir.appendingPathComponent("project.json")
            if let data = try? Data(contentsOf: metaURL),
               let project = try? decoder.decode(AnimationProject.self, from: data) {
                projects.append(project)
            }
        }

        if projects.isEmpty {
            let p1 = createSampleStickmanProject()
            let p2 = createSampleBouncingBallProject()
            saveProject(p1)
            saveProject(p2)
            projects.append(contentsOf: [p1, p2])
        }

        return projects.sorted(by: { $0.modifiedAt > $1.modifiedAt })
    }

    public func saveProject(_ project: AnimationProject) {
        var proj = project
        proj.modifiedAt = Date()

        let projectDir = projectsDirectory.appendingPathComponent(proj.id, isDirectory: true)
        try? fileManager.createDirectory(at: projectDir, withIntermediateDirectories: true)

        let metaURL = projectDir.appendingPathComponent("project.json")
        let encoder = JSONEncoder()
        encoder.outputFormatting = .prettyPrinted
        if let data = try? encoder.encode(proj) {
            try? data.write(to: metaURL)
        }
    }

    public func deleteProject(_ projectId: String) {
        let projectDir = projectsDirectory.appendingPathComponent(projectId, isDirectory: true)
        try? fileManager.removeItem(at: projectDir)
    }

    public func saveLayerImage(projectId: String, frameId: String, layerId: String, image: UIImage) {
        let frameDir = getFrameDirectory(projectId: projectId, frameId: frameId)
        let fileURL = frameDir.appendingPathComponent("layer_\(layerId).png")
        if let data = image.pngData() {
            try? data.write(to: fileURL)
        }
    }

    public func getLayerImage(projectId: String, frameId: String, layerId: String) -> UIImage? {
        let frameDir = getFrameDirectory(projectId: projectId, frameId: frameId)
        let fileURL = frameDir.appendingPathComponent("layer_\(layerId).png")
        guard let data = try? Data(contentsOf: fileURL) else { return nil }
        return UIImage(data: data)
    }

    private func getFrameDirectory(projectId: String, frameId: String) -> URL {
        let dir = projectsDirectory.appendingPathComponent("\(projectId)/frames/\(frameId)", isDirectory: true)
        if !fileManager.fileExists(atPath: dir.path) {
            try? fileManager.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }

    // ==================== Sample Projects ====================

    private func createSampleStickmanProject() -> AnimationProject {
        var project = AnimationProject(
            id: "sample_stickman",
            title: "Project 1 (Stickman)",
            canvasRatio: .ratio1_1,
            fps: 12
        )

        let w: CGFloat = 720
        let h: CGFloat = 720
        let size = CGSize(width: w, height: h)

        for f in 0..<8 {
            let frame = AnimationFrame.createDefault(index: f)
            let renderer = UIGraphicsImageRenderer(size: size)
            let image = renderer.image { ctx in
                let cg = ctx.cgContext
                cg.setLineWidth(14)
                cg.setLineCap(.round)
                cg.setLineJoin(.round)
                UIColor.black.setStroke()
                UIColor.black.setFill()

                // Head
                cg.strokeEllipse(in: CGRect(x: 300, y: 160, width: 120, height: 120))
                // Eyes
                cg.fillEllipse(in: CGRect(x: 335, y: 200, width: 12, height: 12))
                cg.fillEllipse(in: CGRect(x: 375, y: 200, width: 12, height: 12))
                // Body
                cg.move(to: CGPoint(x: 360, y: 280))
                cg.addLine(to: CGPoint(x: 360, y: 440))
                cg.strokePath()

                // Arms
                cg.move(to: CGPoint(x: 360, y: 320))
                cg.addLine(to: CGPoint(x: 290, y: 380))
                cg.move(to: CGPoint(x: 360, y: 320))
                cg.addLine(to: CGPoint(x: 430, y: 360 - CGFloat(f * 4)))
                cg.strokePath()

                // Legs
                cg.move(to: CGPoint(x: 360, y: 440))
                cg.addLine(to: CGPoint(x: 320, y: 580))
                let kickProgress = CGFloat(f) / 7.0
                let kickX = 360.0 + 70.0 + (kickProgress * 90.0)
                let kickY = 440.0 + 140.0 - (kickProgress * 120.0)
                cg.move(to: CGPoint(x: 360, y: 440))
                cg.addLine(to: CGPoint(x: kickX, y: kickY))
                cg.strokePath()

                // Soccer ball
                let ballX = 460.0 + CGFloat(f * 25)
                let ballY = 560.0 - CGFloat(f * 35) + CGFloat(f * f * 2)
                UIColor.orange.setFill()
                cg.fillEllipse(in: CGRect(x: ballX - 28, y: ballY - 28, width: 56, height: 56))
                UIColor.black.setStroke()
                cg.setLineWidth(5)
                cg.strokeEllipse(in: CGRect(x: ballX - 28, y: ballY - 28, width: 56, height: 56))
            }

            if let charLayer = frame.layers.last {
                saveLayerImage(projectId: project.id, frameId: frame.id, layerId: charLayer.id, image: image)
            }
            project.frames.append(frame)
        }

        return project
    }

    private func createSampleBouncingBallProject() -> AnimationProject {
        var project = AnimationProject(
            id: "sample_bouncing_ball",
            title: "Project 2 (Bouncing Ball)",
            canvasRatio: .ratio1_1,
            fps: 12
        )

        let size = CGSize(width: 720, height: 720)
        let ballHeights: [CGFloat] = [200, 250, 340, 460, 580, 460, 340, 250]
        let scalesY: [CGFloat] = [1.0, 1.0, 1.1, 1.2, 0.7, 1.2, 1.1, 1.0]

        for (f, y) in ballHeights.enumerated() {
            let frame = AnimationFrame.createDefault(index: f)
            let renderer = UIGraphicsImageRenderer(size: size)
            let image = renderer.image { ctx in
                let cg = ctx.cgContext
                // Ground
                UIColor.darkGray.setStroke()
                cg.setLineWidth(8)
                cg.move(to: CGPoint(x: 100, y: 620))
                cg.addLine(to: CGPoint(x: 620, y: 620))
                cg.strokePath()

                // Ball
                let sy = scalesY[f]
                let sx = sy < 1.0 ? 1.3 : 0.95
                cg.saveGState()
                cg.translateBy(x: 360, y: y)
                cg.scaleBy(x: sx, y: sy)

                UIColor(red: 0.96, green: 0.75, blue: 0.10, alpha: 1.0).setFill()
                cg.fillEllipse(in: CGRect(x: -45, y: -45, width: 90, height: 90))
                UIColor(white: 0.15, alpha: 1.0).setStroke()
                cg.setLineWidth(8)
                cg.strokeEllipse(in: CGRect(x: -45, y: -45, width: 90, height: 90))
                cg.restoreGState()
            }

            if let charLayer = frame.layers.last {
                saveLayerImage(projectId: project.id, frameId: frame.id, layerId: charLayer.id, image: image)
            }
            project.frames.append(frame)
        }

        return project
    }
}
