import Foundation

public enum DrawingTool: String, CaseIterable, Identifiable, Codable {
    case pen = "Pen"
    case pencil = "Pencil"
    case eraser = "Eraser"
    case fill = "Paint Bucket"

    public var id: String { rawValue }
}
