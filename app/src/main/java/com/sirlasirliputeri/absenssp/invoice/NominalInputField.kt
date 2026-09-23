// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/invoice/NominalInputField.kt
package com.sirlasirliputeri.absenssp.invoice

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.sirlasirliputeri.absenssp.util.RupiahFormatter

/**
 * Kolom input nominal Rupiah dengan format live ##.###.###.
 *
 * PENTING soal bug "input loncat-loncat": itu terjadi karena OutlinedTextField
 * versi (value: String, onValueChange: (String) -> Unit) tidak tahu di mana
 * seharusnya kursor berada setelah teksnya diformat ulang (titik ribuan
 * ditambah/dikurangi tiap ketikan) -- jadi Compose menaruh kursor sembarangan.
 * Perbaikannya: pakai TextFieldValue dan SELALU paksa kursor ke posisi paling
 * akhir setiap kali teks berubah. Untuk input angka polos begini, itu tidak
 * mengganggu karena user memang selalu mengetik/menghapus dari akhir.
 *
 * @param digits nilai mentah (hanya digit, tanpa titik) yang disimpan di state pemanggil.
 * @param onDigitsChange dipanggil dengan digit baru (sudah difilter, dan sudah
 *        dibatasi oleh [maxValue] kalau diisi).
 * @param maxValue kalau diisi, nilai tidak akan bisa diketik melebihi angka ini --
 *        dipakai supaya nominal pembayaran tidak bisa melebihi sisa tagihan.
 */
@Composable
fun NominalInputField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    colors: TextFieldColors,
    shape: Shape,
    modifier: Modifier = Modifier,
    placeholder: String = "0",
    isError: Boolean = false,
    supportingText: String? = null
) {
    val formatted = RupiahFormatter.formatInputDigits(digits)
    val fieldValue = TextFieldValue(text = formatted, selection = TextRange(formatted.length))

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { new ->
            val rawDigits = new.text.filter { it.isDigit() }
            onDigitsChange(rawDigits)
        },
        placeholder = { Text(placeholder) },
        leadingIcon = { Text("Rp") },
        isError = isError,
        supportingText = supportingText?.let { msg -> { Text(msg) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = colors,
        shape = shape,
        modifier = modifier
    )
}

/** Batasi string digit mentah supaya nilainya tidak melebihi [max]. */
fun clampDigitsTo(rawDigits: String, max: Double): String {
    if (max <= 0.0) return ""
    val parsed = rawDigits.toDoubleOrNull() ?: return rawDigits
    return if (parsed > max) Math.floor(max).toLong().toString() else rawDigits
}
