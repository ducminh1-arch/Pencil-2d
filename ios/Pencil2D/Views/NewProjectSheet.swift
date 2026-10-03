import SwiftUI

public struct NewProjectSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var title: String = "Animation \(Int.random(in: 100...999))"
    @State private var selectedRatio: CanvasRatio = .ratio1_1
    @State private var selectedFps: Int = 12

    public var onCreate: (String, CanvasRatio, Int) -> Void

    public init(onCreate: @escaping (String, CanvasRatio, Int) -> Void) {
        self.onCreate = onCreate
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("PROJECT NAME")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.gray)

                        TextField("Project Name", text: $title)
                            .padding(12)
                            .background(Color(white: 0.95))
                            .cornerRadius(10)
                    }

                    // Canvas Sizes (screenshot 3: MULTIPLE CANVAS SIZES)
                    VStack(alignment: .leading, spacing: 8) {
                        Text("MULTIPLE CANVAS SIZES")
                            .font(.system(size: 12, weight: .heavy))
                            .foregroundColor(.gray)

                        HStack(spacing: 10) {
                            ForEach(CanvasRatio.allCases) { ratio in
                                let isSelected = ratio == selectedRatio
                                Button(action: { selectedRatio = ratio }) {
                                    VStack(spacing: 8) {
                                        // Visual aspect box preview
                                        let (w, h) = previewDimensions(for: ratio)
                                        RoundedRectangle(cornerRadius: 3)
                                            .fill(isSelected ? Color(red: 0.96, green: 0.75, blue: 0.10) : Color(white: 0.6))
                                            .frame(width: w, height: h)

                                        Text(ratio.label)
                                            .font(.system(size: 13, weight: isSelected ? .bold : .medium))
                                            .foregroundColor(isSelected ? .black : Color(white: 0.3))
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 16)
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

                    // FPS Selector
                    VStack(alignment: .leading, spacing: 8) {
                        Text("FRAME RATE (FPS)")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.gray)

                        HStack(spacing: 8) {
                            ForEach([6, 12, 16, 24], id: \.self) { fps in
                                let isSelected = fps == selectedFps
                                Button(action: { selectedFps = fps }) {
                                    Text("\(fps) fps")
                                        .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                                        .foregroundColor(isSelected ? .black : .gray)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 10)
                                        .background(isSelected ? Color(red: 0.96, green: 0.75, blue: 0.10) : Color(white: 0.94))
                                        .cornerRadius(8)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    Spacer(minLength: 24)

                    Button(action: {
                        dismiss()
                        onCreate(title, selectedRatio, selectedFps)
                    }) {
                        Text("Create Animation")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(Color(red: 0.96, green: 0.75, blue: 0.10))
                            .cornerRadius(12)
                    }
                }
                .padding(20)
            }
            .navigationTitle("New Animation")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                        .foregroundColor(.black)
                }
            }
        }
    }

    private func previewDimensions(for ratio: CanvasRatio) -> (CGFloat, CGFloat) {
        switch ratio {
        case .ratio1_1: return (24, 24)
        case .ratio16_9: return (32, 18)
        case .ratio4_3: return (28, 21)
        case .ratio9_16: return (18, 32)
        }
    }
}
