data class WordEntity(
    val word: String?,
    val phoneticsymbols: String?,
    val explanation: String?,
    var location: String?, // 格式: "x,y"
    var offsetX: Float = 0f,  // 添加偏移量字段
    var offsetY: Float = 0f   // 添加偏移量字段
) 