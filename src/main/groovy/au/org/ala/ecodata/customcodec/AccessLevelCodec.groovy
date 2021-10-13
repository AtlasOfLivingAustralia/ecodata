package au.org.ala.ecodata.customcodec

import au.org.ala.ecodata.AccessLevel
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext

class AccessLevelCodec implements Codec<AccessLevel> {

    @Override
    AccessLevel decode(BsonReader reader, DecoderContext decoderContext) {
        return AccessLevel.valueOf(reader.readString())
    }

    @Override
    void encode(BsonWriter writer, AccessLevel value, EncoderContext encoderContext) {
        writer.writeString(value.name())
    }

    Class<AccessLevel> getEncoderClass() { AccessLevel }
}
