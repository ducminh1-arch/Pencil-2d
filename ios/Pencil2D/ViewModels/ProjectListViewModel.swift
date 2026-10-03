import SwiftUI
import Combine

@MainActor
public final class ProjectListViewModel: ObservableObject {
    @Published public var projects: [AnimationProject] = []
    @Published public var isLoading: Bool = false

    private let store = ProjectStore.shared

    public init() {
        loadProjects()
    }

    public func loadProjects() {
        isLoading = true
        projects = store.getAllProjects()
        isLoading = false
    }

    public func createProject(title: String, ratio: CanvasRatio, fps: Int) -> AnimationProject {
        var project = AnimationProject(
            title: title.isEmpty ? "Animation \(Int.random(in: 100...999))" : title,
            canvasRatio: ratio,
            fps: fps
        )
        let initialFrame = AnimationFrame.createDefault(index: 0)
        project.frames.append(initialFrame)
        store.saveProject(project)
        loadProjects()
        return project
    }

    @Published public var selectedFilter: String = "all" // "all", "animation", "image"

    public var filteredProjects: [AnimationProject] {
        switch selectedFilter {
        case "image": return projects.filter { $0.isPhotoProject }
        case "animation": return projects.filter { !$0.isPhotoProject }
        default: return projects
        }
    }

    public func setFilter(_ filter: String) {
        selectedFilter = filter
    }

    public func createPhotoProject(title: String, image: UIImage) -> AnimationProject {
        var project = AnimationProject(
            title: title.isEmpty ? "Photo \(Int.random(in: 100...999))" : title,
            canvasRatio: .ratio1_1,
            fps: 1,
            projectType: "image"
        )
        let initialFrame = AnimationFrame.createDefault(index: 0)
        project.frames.append(initialFrame)
        if let data = image.pngData() {
            project.thumbnailData = data
        }
        store.saveProject(project)
        loadProjects()
        return project
    }

    public func deleteProject(id: String) {
        store.deleteProject(id)
        loadProjects()
    }
}
