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
import java.time.Duration;
import java.util.EnumSet;
import org.mariadb.r2dbc.client.ConnectionContext;
import org.mariadb.r2dbc.codec.Codec;
import org.mariadb.r2dbc.codec.DataType;
import org.mariadb.r2dbc.message.server.ColumnDefinitionPacket;
import org.mariadb.r2dbc.util.BufferUtils;

public class DurationCodec implements Codec<Duration> {

  public static final DurationCodec INSTANCE = new DurationCodec();

  private static EnumSet<DataType> COMPATIBLE_TYPES =
      EnumSet.of(DataType.TIME, DataType.DATETIME, DataType.TIMESTAMP);

  public boolean canDecode(ColumnDefinitionPacket column, Class<?> type) {
    return COMPATIBLE_TYPES.contains(column.getDataType()) && type.isAssignableFrom(Duration.class);
  }

  public boolean canEncode(Object value) {
    return value instanceof Duration;
  }

  @Override
  public Duration decodeText(
      ByteBuf buf, int length, ColumnDefinitionPacket column, Class<? extends Duration> type) {

    int[] parts;
    switch (column.getDataType()) {
      case TIMESTAMP:
      case DATETIME:
        parts = LocalDateTimeCodec.parseTimestamp(buf, length);
        if (parts == null) return null;
        return Duration.ZERO
            .plusDays(parts[2] - 1)
            .plusHours(parts[3])
            .plusMinutes(parts[4])
            .plusSeconds(parts[5])
            .plusNanos(parts[6]);

      default:
        parts = LocalTimeCodec.parseTime(buf, length);
        return Duration.ZERO
            .plusHours(parts[0])
            .plusMinutes(parts[1])
            .plusSeconds(parts[2])
            .plusNanos(parts[3]);
    }
  }

  @Override
  public Duration decodeBinary(
      ByteBuf buf, int length, ColumnDefinitionPacket column, Class<? extends Duration> type) {

    long days = 0;
    int hours = 0;
    int minutes = 0;
    int seconds = 0;
    long microseconds = 0;

    switch (column.getDataType()) {
      case TIME:
        boolean negate = false;
        if (length > 0) {
          negate = buf.readUnsignedByte() == 0x01;
          if (length > 4) {
            days = buf.readUnsignedIntLE();
            if (length > 7) {
              hours = buf.readByte();
              minutes = buf.readByte();
              seconds = buf.readByte();
              if (length > 8) {
                microseconds = buf.readIntLE();
              }
            }
          }
        }

        Duration duration =
            Duration.ZERO
                .plusDays(days)
                .plusHours(hours)
                .plusMinutes(minutes)
                .plusSeconds(seconds)
                .plusNanos(microseconds * 1000);
        if (negate) return duration.negated();
        return duration;

      default:
        buf.readUnsignedShortLE(); // skip year
        buf.readByte(); // skip month
        days = buf.readByte();
        if (length > 4) {
          hours = buf.readByte();
          minutes = buf.readByte();
          seconds = buf.readByte();

          if (length > 7) {
            microseconds = buf.readUnsignedIntLE();
          }
        }
        return Duration.ZERO
            .plusDays(days - 1)
            .plusHours(hours)
            .plusMinutes(minutes)
            .plusSeconds(seconds)
            .plusNanos(microseconds * 1000);
    }
  }

  @Override
  public void encodeText(ByteBuf buf, ConnectionContext context, Duration value) {
    BufferUtils.write(buf, value);
  }

  @Override
  public void encodeBinary(ByteBuf buf, ConnectionContext context, Duration value) {
    int nano = value.getNano();
    if (nano > 0) {
      buf.writeByte((byte) 12);
      buf.writeByte((byte) (value.isNegative() ? 1 : 0));
      buf.writeIntLE((int) value.toDays());
      buf.writeByte((byte) (value.toHours() - 24 * value.toDays()));
      buf.writeByte((byte) (value.toMinutes() - 60 * value.toHours()));
      buf.writeByte((byte) (value.getSeconds() - 60 * value.toMinutes()));
      buf.writeIntLE(nano / 1000);
    } else {
      buf.writeByte((byte) 8);
      buf.writeByte((byte) (value.isNegative() ? 1 : 0));
      buf.writeIntLE((int) value.toDays());
      buf.writeByte((byte) (value.toHours() - 24 * value.toDays()));
      buf.writeByte((byte) (value.toMinutes() - 60 * value.toHours()));
      buf.writeByte((byte) (value.getSeconds() - 60 * value.toMinutes()));
    }
  }

  public DataType getBinaryEncodeType() {
    return DataType.TIME;
  }

  @Override
  public String toString() {
    return "DurationCodec{}";
  }
}
