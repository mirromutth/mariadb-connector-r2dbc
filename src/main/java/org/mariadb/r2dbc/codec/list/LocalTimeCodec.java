/*
 * Copyright 2020 MariaDB Ab.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mariadb.r2dbc.codec.list;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.EnumSet;
import org.mariadb.r2dbc.client.ConnectionContext;
import org.mariadb.r2dbc.codec.Codec;
import org.mariadb.r2dbc.codec.DataType;
import org.mariadb.r2dbc.message.server.ColumnDefinitionPacket;
import org.mariadb.r2dbc.util.BufferUtils;

public class LocalTimeCodec implements Codec<LocalTime> {

  public static final LocalTimeCodec INSTANCE = new LocalTimeCodec();

  private static EnumSet<DataType> COMPATIBLE_TYPES =
      EnumSet.of(DataType.TIME, DataType.DATETIME, DataType.TIMESTAMP);

  public static int[] parseTime(ByteBuf buf, int length) {
    String raw = buf.readCharSequence(length, StandardCharsets.UTF_8).toString();
    boolean negate = raw.startsWith("-");
    if (negate) {
      raw = raw.substring(1);
    }
    String[] rawPart = raw.split(":");
    if (rawPart.length == 3) {
      int hour = Integer.parseInt(rawPart[0]);
      int minutes = Integer.parseInt(rawPart[1]);
      int seconds = Integer.parseInt(rawPart[2].substring(0, 2));
      int nanoseconds = extractNanos(raw);

      return new int[] {hour, minutes, seconds, nanoseconds};

    } else {
      throw new IllegalArgumentException(
          String.format(
              "%s cannot be parse as time. time must have" + " \"99:99:99\" format", raw));
    }
  }

  protected static int extractNanos(String timestring) {
    int index = timestring.indexOf('.');
    if (index == -1) {
      return 0;
    }
    int nanos = 0;
    for (int i = index + 1; i < index + 10; i++) {
      int digit;
      if (i >= timestring.length()) {
        digit = 0;
      } else {
        digit = timestring.charAt(i) - '0';
      }
      nanos = nanos * 10 + digit;
    }
    return nanos;
  }

  public boolean canDecode(ColumnDefinitionPacket column, Class<?> type) {
    return COMPATIBLE_TYPES.contains(column.getDataType())
        && type.isAssignableFrom(LocalTime.class);
  }

  public boolean canEncode(Object value) {
    return value instanceof LocalTime;
  }

  @Override
  public LocalTime decodeText(
      ByteBuf buf, int length, ColumnDefinitionPacket column, Class<? extends LocalTime> type) {

    int[] parts;
    switch (column.getDataType()) {
      case TIMESTAMP:
      case DATETIME:
        parts = LocalDateTimeCodec.parseTimestamp(buf, length);
        if (parts == null) return null;
        return LocalTime.of(parts[3], parts[4], parts[5], parts[6]);

      default:
        parts = parseTime(buf, length);
        return LocalTime.of(parts[0] % 24, parts[1], parts[2], parts[3]);
    }
  }

  @Override
  public LocalTime decodeBinary(
      ByteBuf buf, int length, ColumnDefinitionPacket column, Class<? extends LocalTime> type) {

    int hour = 0;
    int minutes = 0;
    int seconds = 0;
    long microseconds = 0;
    switch (column.getDataType()) {
      case TIMESTAMP:
      case DATETIME:
        buf.skipBytes(4); // skip year, month and day
        if (length > 4) {
          hour = buf.readByte();
          minutes = buf.readByte();
          seconds = buf.readByte();

          if (length > 7) {
            microseconds = buf.readIntLE();
          }
        }
        return LocalTime.of(hour, minutes, seconds).plusNanos(microseconds * 1000);

      default: // TIME
        buf.skipBytes(1); // skip negate
        if (length > 4) {
          buf.skipBytes(4); // skip days
          if (length > 7) {
            hour = buf.readByte();
            minutes = buf.readByte();
            seconds = buf.readByte();
            if (length > 8) {
              microseconds = buf.readIntLE();
            }
          }
        }
        return LocalTime.of(hour, minutes, seconds).plusNanos(microseconds * 1000);
    }
  }

  @Override
  public void encodeText(ByteBuf buf, ConnectionContext context, LocalTime value) {
    BufferUtils.write(buf, value);
  }

  @Override
  public void encodeBinary(ByteBuf buf, ConnectionContext context, LocalTime value) {
    int nano = value.getNano();
    if (nano > 0) {
      buf.writeByte((byte) 12);
      buf.writeByte((byte) 0);
      buf.writeIntLE(0);
      buf.writeByte((byte) value.get(ChronoField.HOUR_OF_DAY));
      buf.writeByte((byte) value.get(ChronoField.MINUTE_OF_HOUR));
      buf.writeByte((byte) value.get(ChronoField.SECOND_OF_MINUTE));
      buf.writeIntLE(nano / 1000);
    } else {
      buf.writeByte((byte) 8);
      buf.writeByte((byte) 0);
      buf.writeIntLE(0);
      buf.writeByte((byte) value.get(ChronoField.HOUR_OF_DAY));
      buf.writeByte((byte) value.get(ChronoField.MINUTE_OF_HOUR));
      buf.writeByte((byte) value.get(ChronoField.SECOND_OF_MINUTE));
    }
  }

  public DataType getBinaryEncodeType() {
    return DataType.TIME;
  }

  @Override
  public String toString() {
    return "LocalTimeCodec{}";
  }
}
