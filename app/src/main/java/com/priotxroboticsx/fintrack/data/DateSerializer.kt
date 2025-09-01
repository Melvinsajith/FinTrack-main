package com.priotxroboticsx.fintrack.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Date

// This custom serializer tells the Kotlinx Serialization library how to handle
// the 'java.util.Date' type, which it doesn't know how to process by default.
object DateSerializer : KSerializer<Date> {
    // We describe the Date as a primitive LONG type, since we'll store it as a timestamp.
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)

    // When saving (serializing), we take the Date object and get its timestamp (a Long).
    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeLong(value.time)
    }

    // When loading (deserializing), we read the Long timestamp from the JSON
    // and use it to create a new Date object.
    override fun deserialize(decoder: Decoder): Date {
        return Date(decoder.decodeLong())
    }
}

