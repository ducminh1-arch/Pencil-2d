import SwiftUI

public struct ExportSheet: View {
    @ObservedObject var viewModel: EditorViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var selectedFormat: ExportFormat = .mp4
    @State private var selectedFps: Int
    @State private var loopCount: Int = 2
    @State private var showShareSheet: Bool = false
    @State private var shareURL: URL? = nil

    public init(viewModel: EditorViewModel) {
        self.viewModel = viewModel
        self._selectedFps = State(initialValue: viewModel.project.fps)
    }

    public var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 20) {
                if viewModel.isExporting {
                    VStack(spacing: 16) {
                        Spacer()
                        ProgressView()
                            .scaleEffect(1.4)
                            .tint(Color(red: 0.96, green: 0.75, blue: 0.10))

                        Text(viewModel.exportMessage ?? "Exporting animation...")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(.gray)
                        Spacer()
                    }
                    .frame(maxWidth: .infinity)
                } else {
                    // Format selection
                    VStack(alignment: .leading, spacing: 8) {
                        Text("EXPORT FORMAT")
                            .font(.system(size: 12, weight: .heavy))
                            .foregroundColor(.gray)

                        HStack(spacing: 10) {
                            ForEach(ExportFormat.allCases) { format in
                                let isSelected = format == selectedFormat
                                Button(action: { selectedFormat = format }) {
                                    VStack(spacing: 8) {
                                        Image(systemName: iconName(for: format))
                                            .font(.system(size: 24))
                                            .foregroundColor(isSelected ? Color(red: 0.96, green: 0.75, blue: 0.10) : .gray)

                                        Text(format.label)
                                            .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                                            .foregroundColor(isSelected ? .black : .gray)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                                    .background(isSelected ? Color(red: 0.96, green: 0.75, blue: 0.10).opacity(0.18) : Color(white: 0.96))
                                    .cornerRadius(12)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(isSelected ? Color(red: 0.96, green: 0.75, blue: 0.10) : Color.clear, lineWidth: 2)
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    // FPS
                    VStack(alignment: .leading, spacing: 8) {
                        Text("FRAME RATE (FPS)")
                            .font(.system(size: 12, weight: .heavy))
                            .foregroundColor(.gray)

                        HStack(spacing: 8) {
                            ForEach([12, 24, 30], id: \.self) { fps in
                                let isSelected = fps == selectedFps
                                Button(action: { selectedFps = fps }) {
                                    Text("\(fps) fps")
                                        .font(.system(size: 13, weight: isSelected ? .bold : .medium))
                                        .foregroundColor(isSelected ? .black : .gray)
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 8)
                                        .background(isSelected ? Color(red: 0.96, green: 0.75, blue: 0.10) : Color(white: 0.94))
                                        .cornerRadius(8)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    if selectedFormat == .mp4 {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("REPEAT LOOPS: \(loopCount) times")
                                .font(.system(size: 12, weight: .heavy))
                                .foregroundColor(.gray)

                            Stepper(value: $loopCount, in: 1...10) {
                                Text("\(loopCount) loops")
                                    .font(.system(size: 14))
                            }
                        }
                    }

                    Spacer()

                    Button(action: {
                        let opt = ExportOption(format: selectedFormat, fps: selectedFps, loopCount: loopCount)
                        viewModel.exportAnimation(option: opt) { fileURL in
                            if let url = fileURL {
                                shareURL = url
                                showShareSheet = true
                            }
                        }
                    }) {
                        HStack {
                            Image(systemName: "square.and.arrow.up")
                            Text("Export & Share")
                                .fontWeight(.bold)
                        }
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color(red: 0.96, green: 0.75, blue: 0.10))
                        .cornerRadius(12)
                    }
                }
            }
            .padding(20)
            .navigationTitle("Export Animation")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") { dismiss() }
                        .foregroundColor(.black)
                }
            }
            .sheet(isPresented: $showShareSheet) {
                if let url = shareURL {
                    ShareSheet(activityItems: [url])
                }
            }
        }
    }

    private func iconName(for format: ExportFormat) -> String {
        switch format {
        case .mp4: return "video.fill"
        case .gif: return "photo.stack.fill"
        case .pngSequence: return "photo.on.rectangle.angled"
        }
    }
}

struct ShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
