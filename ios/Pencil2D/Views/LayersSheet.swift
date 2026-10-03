import SwiftUI

public struct LayersSheet: View {
    @ObservedObject var viewModel: EditorViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var newLayerName: String = ""
    @State private var showAddAlert = false

    public var body: some View {
        NavigationStack {
            VStack {
                let currentFrame = viewModel.project.frames[safe: viewModel.currentFrameIndex]
                let layers = currentFrame?.layers ?? []

                List {
                    ForEach(Array(layers.enumerated().reversed()), id: \.element.id) { index, layer in
                        let isSelected = index == viewModel.currentLayerIndex

                        VStack(spacing: 8) {
                            HStack {
                                Image(systemName: "square.3.layers.3d")
                                    .foregroundColor(isSelected ? Color(red: 0.96, green: 0.75, blue: 0.10) : .gray)

                                Text(layer.name)
                                    .font(.system(size: 15, weight: isSelected ? .bold : .medium))

                                Spacer()

                                Button(action: { viewModel.toggleLayerVisibility(at: index) }) {
                                    Image(systemName: layer.isVisible ? "eye" : "eye.slash")
                                        .foregroundColor(layer.isVisible ? .primary : .gray)
                                }
                                .buttonStyle(.borderless)

                                Button(action: { viewModel.toggleLayerLock(at: index) }) {
                                    Image(systemName: layer.isLocked ? "lock.fill" : "lock.open")
                                        .foregroundColor(layer.isLocked ? .red : .gray)
                                }
                                .buttonStyle(.borderless)
                            }

                            HStack {
                                Text("Opacity: \(Int(layer.opacity * 100))%")
                                    .font(.system(size: 11))
                                    .foregroundColor(.gray)
                                    .frame(width: 80, alignment: .leading)

                                Slider(
                                    value: Binding(
                                        get: { Double(layer.opacity) },
                                        set: { viewModel.setLayerOpacity(at: index, opacity: Float($0)) }
                                    ),
                                    in: 0.0...1.0
                                )
                                .tint(Color(red: 0.96, green: 0.75, blue: 0.10))
                            }
                        }
                        .padding(.vertical, 4)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            viewModel.selectLayer(index)
                        }
                    }
                }
            }
            .navigationTitle("Layers")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { showAddAlert = true }) {
                        Image(systemName: "plus")
                            .foregroundColor(.black)
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                        .fontWeight(.bold)
                        .foregroundColor(.black)
                }
            }
            .alert("Add Layer", isPresented: $showAddAlert) {
                TextField("Layer Name", text: $newLayerName)
                Button("Add") {
                    viewModel.addLayer(name: newLayerName)
                    newLayerName = ""
                }
                Button("Cancel", role: .cancel) {}
            }
        }
    }
}
