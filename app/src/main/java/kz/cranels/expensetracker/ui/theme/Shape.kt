package kz.cranels.expensetracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp), // This will apply to OutlinedTextField
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)
