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

package org.mariadb.r2dbc.integration.parameter;

import io.r2dbc.spi.Blob;
import io.r2dbc.spi.R2dbcBadGrammarException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.BitSet;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mariadb.r2dbc.BaseTest;
import org.mariadb.r2dbc.api.MariadbConnection;
import org.mariadb.r2dbc.api.MariadbResult;
import org.mariadb.r2dbc.api.MariadbStatement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class BitParameterTest extends BaseTest {
  @BeforeAll
  public static void before2() {
    sharedConn
        .createStatement("CREATE TABLE ByteParam (t1 BIT(4), t2 BIT(20), t3 BIT(1))")
        .execute()
        .blockLast();
    // ensure having same kind of result for truncation
    sharedConn
        .createStatement("SET @@sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION'")
        .execute()
        .blockLast();
  }

  @AfterAll
  public static void afterAll2() {
    sharedConn.createStatement("DROP TABLE ByteParam").execute().blockLast();
  }

  @BeforeEach
  public void beforeEach() {
    sharedConn.createStatement("TRUNCATE TABLE ByteParam").execute().blockLast();
  }

  @Test
  void nullValue() {
    nullValue(sharedConn);
  }

  @Test
  void nullValuePrepare() {
    nullValue(sharedConnPrepare);
  }

  private void nullValue(MariadbConnection connection) {
    connection
        .createStatement("INSERT INTO ByteParam VALUES (?,?,?)")
        .bindNull(0, Byte.class)
        .bindNull(1, Byte.class)
        .bindNull(2, Byte.class)
        .execute()
        .blockLast();
    validate(Optional.empty(), Optional.empty(), Optional.empty());
  }

  @Test
  void booleanValue() {
    booleanValue(sharedConn);
  }

  @Test
  void booleanValuePrepare() {
    booleanValue(sharedConnPrepare);
  }

  private void booleanValue(MariadbConnection connection) {
    MariadbStatement stmt =
        connection
            .createStatement("INSERT INTO ByteParam VALUES (?,?,?)")
            .bind(0, Boolean.TRUE)
            .bind(1, Boolean.TRUE)
            .bind(2, Boolean.FALSE);
    Assertions.assertTrue(
        stmt.toString()
                .contains(
                    "parameters=[Parameter{codec=BooleanCodec{}, value=true}, Parameter{codec=BooleanCodec{}, value=true}, Parameter{codec=BooleanCodec{}, value=false}]")
            || stmt.toString()
                .contains(
                    "parameters={0=Parameter{codec=BooleanCodec{}, value=true}, 1=Parameter{codec=BooleanCodec{}, value=true}, 2=Parameter{codec=BooleanCodec{}, value=false}}"));
    stmt.execute().blockLast();
    validate(
        Optional.of(BitSet.valueOf(new byte[] {(byte) 1})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 1})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 0})));
  }

  @Test
  void bigIntValue() {
    bigIntValue(sharedConn);
  }

  @Test
  void bigIntValuePrepare() {
    bigIntValue(sharedConnPrepare);
  }

  private void bigIntValue(MariadbConnection connection) {
    connection
        .createStatement("INSERT INTO ByteParam VALUES (?,?,?)")
        .bind(0, new BigInteger("11"))
        .bind(1, new BigInteger("512"))
        .bind(2, new BigInteger("1"))
        .execute()
        .blockLast();
    validate(
        Optional.of(BitSet.valueOf(new byte[] {(byte) 11})),
        Optional.of(BitSet.valueOf(new byte[] {0, (byte) 2, 0})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 1})));
  }

  @Test
  void stringValue() {
    stringValue(sharedConn);
  }

  @Test
  void stringValuePrepare() {
    stringValue(sharedConnPrepare);
  }

  private void stringValue(MariadbConnection connection) {
    connection
        .createStatement("INSERT INTO ByteParam VALUES (?,?,?)")
        .bind(0, "\1")
        .bind(1, "A")
        .bind(2, "\0")
        .execute()
        .blockLast();
    validate(
        Optional.of(BitSet.valueOf(new byte[] {(byte) 1})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 65, 0, 0})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 0})));
  }

  @Test
  void decimalValue() {
    decimalValue(sharedConn);
  }

  @Test
  void decimalValuePrepare() {
    decimalValue(sharedConnPrepare);
  }

  private void decimalValue(MariadbConnection connection) {
    connection
        .createStatement("INSERT INTO ByteParam VALUES (?,?,?)")
        .bind(0, new BigDecimal("11"))
        .bind(1, new BigDecimal("512"))
        .bind(2, new BigDecimal("1"))
        .execute()
        .blockLast();
    validate(
        Optional.of(BitSet.valueOf(new byte[] {(byte) 11})),
        Optional.of(BitSet.valueOf(new byte[] {0, (byte) 2, 0})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 1})));
  }

  @Test
  void intValue() {
    intValue(sharedConn);
  }

  @Test
  void intValuePrepare() {
    intValue(sharedConnPrepare);
  }

  private void intValue(MariadbConnection connection) {
    connection
        .createStatement("INSERT INTO ByteParam VALUES (?,?,?)")
        .bind(0, 11)
        .bind(1, 512)
        .bind(2, 1)
        .execute()
        .blockLast();
    validate(
        Optional.of(BitSet.valueOf(new byte[] {(byte) 11})),
        Optional.of(BitSet.valueOf(new byte[] {0, (byte) 2, 0})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 1})));
  }

  @Test
  void byteValue() {
    byteValue(sharedConn);
  }

  @Test
  void byteValuePrepare() {
    byteValue(sharedConnPrepare);
  }

  private void byteValue(MariadbConnection connection) {
    connection
        .createStatement("INSERT INTO ByteParam VALUES (?,?,?)")
        .bind(0, (byte) 15)
        .bind(1, (byte) 127)
        .bind(2, (byte) 0)
        .execute()
        .blockLast();
    validate(
        Optional.of(BitSet.valueOf(new byte[] {(byte) 15})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 127, 0, 0})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 0})));
  }

  @Test
  void blobValue() {
    blobValue(sharedConn);
  }

  @Test
  void blobValuePrepare() {
    blobValue(sharedConnPrepare);
  }

  private void blobValue(MariadbConnection connection) {
    connection
        .createStatement("INSERT INTO ByteParam VALUES (?,?,?)")
        .bind(0, Blob.from(Mono.just(ByteBuffer.wrap(new byte[] {(byte) 15}))))
        .bind(1, Blob.from(Mono.just(ByteBuffer.wrap(new byte[] {(byte) 1, 0, (byte) 127}))))
        .bind(2, Blob.from(Mono.just(ByteBuffer.wrap(new byte[] {0}))))
        .execute()
        .blockLast();
    validate(
        Optional.of(BitSet.valueOf(new byte[] {(byte) 15})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 127, 0, (byte) 1})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 0})));
  }

  @Test
  void floatValue() {
    floatValue(sharedConn);
  }

  @Test
  void floatValuePrepare() {
    floatValue(sharedConnPrepare);
  }

  private void floatValue(MariadbConnection connection) {
    connection
        .createStatement("INSERT INTO ByteParam VALUES (?,?,?)")
        .bind(0, 11f)
        .bind(1, 512f)
        .bind(2, 1f)
        .execute()
        .blockLast();
    validate(
        Optional.of(BitSet.valueOf(new byte[] {(byte) 11})),
        Optional.of(BitSet.valueOf(new byte[] {0, (byte) 2, 0})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 1})));
  }

  @Test
  void doubleValue() {
    doubleValue(sharedConn);
  }

  @Test
  void doubleValuePrepare() {
    doubleValue(sharedConnPrepare);
  }

  private void doubleValue(MariadbConnection connection) {
    connection
        .createStatement("INSERT INTO ByteParam VALUES (?,?,?)")
        .bind(0, 11d)
        .bind(1, 512d)
        .bind(2, 1d)
        .execute()
        .blockLast();
    validate(
        Optional.of(BitSet.valueOf(new byte[] {(byte) 11})),
        Optional.of(BitSet.valueOf(new byte[] {0, (byte) 2, 0})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 1})));
  }

  @Test
  void shortValue() {
    shortValue(sharedConn);
  }

  @Test
  void shortValuePrepare() {
    shortValue(sharedConnPrepare);
  }

  private void shortValue(MariadbConnection connection) {
    connection
        .createStatement("INSERT INTO ByteParam VALUES (?,?,?)")
        .bind(0, Short.valueOf("11"))
        .bind(1, Short.valueOf("127"))
        .bind(2, Short.valueOf("1"))
        .execute()
        .blockLast();
    validate(
        Optional.of(BitSet.valueOf(new byte[] {(byte) 11})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 127, 0, 0})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 1})));
  }

  @Test
  void longValue() {
    longValue(sharedConn);
  }

  @Test
  void longValuePrepare() {
    longValue(sharedConnPrepare);
  }

  private void longValue(MariadbConnection connection) {
    connection
        .createStatement("INSERT INTO ByteParam VALUES (?,?,?)")
        .bind(0, 11L)
        .bind(1, 512L)
        .bind(2, 1L)
        .execute()
        .blockLast();
    validate(
        Optional.of(BitSet.valueOf(new byte[] {(byte) 11})),
        Optional.of(BitSet.valueOf(new byte[] {0, (byte) 2, 0})),
        Optional.of(BitSet.valueOf(new byte[] {(byte) 1})));
  }

  @Test
  void localDateTimeValue() {
    localDateTimeValue(sharedConn);
  }

  @Test
  void localDateTimeValuePrepare() {
    localDateTimeValue(sharedConnPrepare);
  }

  private void localDateTimeValue(MariadbConnection connection) {
    Flux<MariadbResult> f =
        connection
            .createStatement("INSERT INTO ByteParam VALUES (?,?,?)")
            .bind(0, LocalDateTime.now())
            .bind(1, LocalDateTime.now())
            .bind(2, LocalDateTime.now())
            .execute();
    if ((isMariaDBServer() && !minVersion(10, 2, 0))
        || (!isMariaDBServer() && !minVersion(5, 7, 0))) {
      f.blockLast();
    } else {
      f.flatMap(r -> r.getRowsUpdated())
          .as(StepVerifier::create)
          .expectErrorMatches(
              throwable ->
                  throwable instanceof R2dbcBadGrammarException
                      && ((R2dbcBadGrammarException) throwable).getSqlState().equals("22001"))
          .verify();
    }
  }

  @Test
  void localDateValue() {
    localDateValue(sharedConn);
  }

  @Test
  void localDateValuePrepare() {
    localDateValue(sharedConnPrepare);
  }

  private void localDateValue(MariadbConnection connection) {
    Flux<MariadbResult> f =
        connection
            .createStatement("INSERT INTO ByteParam VALUES (?,?,?)")
            .bind(0, LocalDate.now())
            .bind(1, LocalDate.now())
            .bind(2, LocalDate.now())
            .execute();
    if ((isMariaDBServer() && !minVersion(10, 2, 0))
        || (!isMariaDBServer() && !minVersion(5, 7, 0))) {
      f.blockLast();
    } else {
      f.flatMap(r -> r.getRowsUpdated())
          .as(StepVerifier::create)
          .expectErrorMatches(
              throwable ->
                  throwable instanceof R2dbcBadGrammarException
                      && ((R2dbcBadGrammarException) throwable).getSqlState().equals("22001"))
          .verify();
    }
  }

  @Test
  void localTimeValue() {
    localTimeValue(sharedConn);
  }

  @Test
  void localTimeValuePrepare() {
    localTimeValue(sharedConnPrepare);
  }

  private void localTimeValue(MariadbConnection connection) {
    Flux<MariadbResult> f =
        connection
            .createStatement("INSERT INTO ByteParam VALUES (?,?,?)")
            .bind(0, LocalTime.now())
            .bind(1, LocalTime.now())
            .bind(2, LocalTime.now())
            .execute();
    if ((isMariaDBServer() && !minVersion(10, 2, 0))
        || (!isMariaDBServer() && !minVersion(5, 7, 0))) {
      f.blockLast();
    } else {
      f.flatMap(r -> r.getRowsUpdated())
          .as(StepVerifier::create)
          .expectErrorMatches(
              throwable ->
                  throwable instanceof R2dbcBadGrammarException
                      && ((R2dbcBadGrammarException) throwable).getSqlState().equals("22001"))
          .verify();
    }
  }

  private void validate(Optional<BitSet> t1, Optional<BitSet> t2, Optional<BitSet> t3) {
    sharedConn
        .createStatement("SELECT * FROM ByteParam")
        .execute()
        .flatMap(
            r ->
                r.map(
                    (row, metadata) -> {
                      BitSet obj0 = (BitSet) row.get(0);
                      BitSet obj1 = (BitSet) row.get(1);
                      BitSet obj2 = (BitSet) row.get(2);
                      return Flux.just(
                          Optional.ofNullable(obj0),
                          Optional.ofNullable(obj1),
                          Optional.ofNullable(obj2));
                    }))
        .blockLast()
        .as(StepVerifier::create)
        .expectNext(t1, t2, t3)
        .verifyComplete();
  }
}
