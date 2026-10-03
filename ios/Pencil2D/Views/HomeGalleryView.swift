import SwiftUI
import PhotosUI

public struct HomeGalleryView: View {
    @StateObject private var viewModel = ProjectListViewModel()
    @State private var showNewProjectSheet = false
    @State private var selectedProject: AnimationProject?
    @State private var selectedPhotoItem: PhotosPickerItem? = nil

    let columns = [
        GridItem(.flexible(), spacing: 14),
        GridItem(.flexible(), spacing: 14)
    ]

    public init() {}

    public var body: some View {
        NavigationStack {
            ZStack {
                // Graph Paper Background matching Phone 5
                GraphPaperBackground()
                    .ignoresSafeArea()

                VStack(spacing: 0) {
                    // Centered Header Title matching App Store
                    Text("MAKE EASY ANIMATION")
                        .font(.system(size: 22, weight: .heavy, design: .serif))
                        .foregroundColor(Color(red: 0.12, green: 0.12, blue: 0.14))
                        .padding(.top, 14)
                        .padding(.bottom, 10)

                    // Categories Filter Tabs + Quick Import Chip: All on single row, no scroll!
                    HStack {
                        HStack(spacing: 6) {
                            FilterChipView(
                                title: "All",
                                isSelected: viewModel.selectedFilter == "all",
                                selectedColor: Color(red: 0.96, green: 0.75, blue: 0.10),
                                selectedTextColor: .black
                            ) {
                                viewModel.setFilter("all")
                            }

                            FilterChipView(
                                title: "🎬 Animation",
                                isSelected: viewModel.selectedFilter == "animation",
                                selectedColor: Color(red: 1.0, green: 0.18, blue: 0.33),
                                selectedTextColor: .white
                            ) {
                                viewModel.setFilter("animation")
                            }

                            FilterChipView(
                                title: "🖼️ Photo",
                                isSelected: viewModel.selectedFilter == "image",
                                selectedColor: Color(red: 0.10, green: 0.45, blue: 0.91),
                                selectedTextColor: .white
                            ) {
                                viewModel.setFilter("image")
                            }
                        }

                        Spacer()

                        // Quick Import Action Chip
                        PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                            HStack(spacing: 4) {
                                Image(systemName: "photo.badge.plus")
                                    .font(.system(size: 12, weight: .bold))
                                Text("+ Import")
                                    .font(.system(size: 11.5, weight: .bold))
                            }
                            .padding(.horizontal, 9)
                            .padding(.vertical, 6)
                            .foregroundColor(Color(red: 0.10, green: 0.45, blue: 0.91))
                            .background(Color(red: 0.91, green: 0.94, blue: 1.0))
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(Color(red: 0.10, green: 0.45, blue: 0.91), lineWidth: 1)
                            )
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 12)

                    if viewModel.filteredProjects.isEmpty {
                        VStack(spacing: 10) {
                            Spacer()
                            Text(viewModel.selectedFilter == "image" ? "🖼️" : "📂")
                                .font(.system(size: 42))
                            Text(viewModel.selectedFilter == "image" ? "No photo projects yet" : "No projects in this category")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(Color(white: 0.4))
                            if viewModel.selectedFilter == "image" {
                                PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                                    HStack(spacing: 6) {
                                        Image(systemName: "photo.badge.plus")
                                        Text("Import Photo")
                                    }
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .background(Color(red: 0.10, green: 0.45, blue: 0.91))
                                    .cornerRadius(10)
                                }
                                .padding(.top, 4)
                            }
                            Spacer()
                        }
                    } else {
                    ScrollView {
                        LazyVGrid(columns: columns, spacing: 14) {
                            ForEach(viewModel.filteredProjects) { project in
                                NotebookProjectCardView(project: project) {
                                    selectedProject = project
                                } onDelete: {
                                    viewModel.deleteProject(id: project.id)
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 90)
                    }
                    }
                }

                // Floating Pencil Button (+)
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        Button(action: { showNewProjectSheet = true }) {
                            Image(systemName: "plus")
                                .font(.system(size: 22, weight: .heavy))
                                .foregroundColor(.black)
                                .frame(width: 54, height: 54)
                                .background(Color(red: 0.96, green: 0.75, blue: 0.10))
                                .clipShape(Circle())
                                .overlay(Circle().stroke(Color.black, lineWidth: 2))
                                .shadow(color: Color.black.opacity(0.25), radius: 3, x: 2, y: 3)
                        }
                        .padding(.trailing, 20)
                        .padding(.bottom, 20)
                    }
                }
            }
            .navigationBarHidden(true)
            .sheet(isPresented: $showNewProjectSheet) {
                NewProjectSheet { title, ratio, fps in
                    let newProj = viewModel.createProject(title: title, ratio: ratio, fps: fps)
                    selectedProject = newProj
                }
            }
            .fullScreenCover(item: $selectedProject) { proj in
                EditorView(project: proj) {
                    selectedProject = nil
                    viewModel.loadProjects()
                }
            }
            .onChange(of: selectedPhotoItem) { newItem in
                Task {
                    if let data = try? await newItem?.loadTransferable(type: Data.self),
                       let uiImage = UIImage(data: data) {
                        let newProj = viewModel.createPhotoProject(title: "Photo \(Int.random(in: 100...999))", image: uiImage)
                        selectedProject = newProj
                    }
                }
            }
        }
    }
}

struct FilterChipView: View {
    let title: String
    let isSelected: Bool
    let selectedColor: Color
    let selectedTextColor: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 12, weight: .bold))
                .padding(.horizontal, 14)
                .padding(.vertical, 7)
                .background(isSelected ? selectedColor : Color(white: 0.93))
                .foregroundColor(isSelected ? selectedTextColor : Color(red: 0.2, green: 0.2, blue: 0.2))
                .cornerRadius(12)
        }
        .buttonStyle(.plain)
    }
}

struct GraphPaperBackground: View {
    var body: some View {
        GeometryReader { geometry in
            Path { path in
                let step: CGFloat = 24.0
                for x in stride(from: 0, to: geometry.size.width, by: step) {
                    path.move(to: CGPoint(x: x, y: 0))
                    path.addLine(to: CGPoint(x: x, y: geometry.size.height))
                }
                for y in stride(from: 0, to: geometry.size.height, by: step) {
                    path.move(to: CGPoint(x: 0, y: y))
                    path.addLine(to: CGPoint(x: geometry.size.width, y: y))
                }
            }
            .stroke(Color(red: 0.91, green: 0.89, blue: 0.84), lineWidth: 1)
            .background(Color(red: 0.98, green: 0.97, blue: 0.95))
        }
    }
}

struct NotebookProjectCardView: View {
    let project: AnimationProject
    let onOpen: () -> Void
    let onDelete: () -> Void

    var body: some View {
        Button(action: onOpen) {
            VStack(alignment: .leading, spacing: 0) {
                // Card Top Title
                Text(project.title)
                    .font(.system(size: 15, weight: .bold, design: .serif))
                    .foregroundColor(Color(red: 0.2, green: 0.2, blue: 0.2))
                    .padding(.horizontal, 10)
                    .padding(.top, 8)
                    .padding(.bottom, 2)
                    .lineLimit(1)

                // Thumbnail Canvas
                ZStack(alignment: .topLeading) {
                    Color.white
                        .frame(height: 110)

                    if let data = project.thumbnailData, let uiImg = UIImage(data: data) {
                        Image(uiImage: uiImg)
                            .resizable()
                            .scaledToFit()
                            .frame(height: 110)
                    } else {
                        HStack {
                            Spacer()
                            Image(systemName: project.isPhotoProject ? "photo" : "paintbrush.pointed")
                                .font(.system(size: 32))
                                .foregroundColor(Color(white: 0.8))
                            Spacer()
                        }
                    }

                    // Type Badge
                    Text(project.isPhotoProject ? "🖼️ PHOTO" : "🎬 ANIMATION")
                        .font(.system(size: 9, weight: .heavy))
                        .foregroundColor(.white)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(project.isPhotoProject ? Color(red: 0.10, green: 0.45, blue: 0.91) : Color(red: 1.0, green: 0.18, blue: 0.33))
                        .cornerRadius(6)
                        .padding(6)
                }

                // Card Bottom Info
                HStack {
                    Text(project.isPhotoProject ? "PNG • 1 Frame" : "MP4 : \(project.fps) fps")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(project.isPhotoProject ? Color(red: 0.10, green: 0.45, blue: 0.91) : Color(white: 0.45))

                    Spacer()

                    Menu {
                        Button("Open Animation", action: onOpen)
                        Button(role: .destructive, action: onDelete) {
                            Label("Delete", systemImage: "trash")
                        }
                    } label: {
                        Text("⋮")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(Color(white: 0.5))
                            .padding(4)
                    }
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 6)
                .background(Color.white)
            }
            .background(Color.white)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color(red: 0.15, green: 0.15, blue: 0.15), lineWidth: 1.5)
            )
            .shadow(color: Color.black.opacity(0.12), radius: 2, x: 2, y: 3)
        }
        .buttonStyle(.plain)
    }
}
