package components.chat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.List;

public class ContactsListSerializer extends StdSerializer<List<User>> {
    public ContactsListSerializer() {
        this(null);
    }

    public ContactsListSerializer(Class<List<User>> t) {
        super(t);
    }

    @Override
    public void serialize(List<User> value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartArray();

        for (User u : value) {
            gen.writeNumber(u.getId());
        }

        gen.writeEndArray();
    }
}
