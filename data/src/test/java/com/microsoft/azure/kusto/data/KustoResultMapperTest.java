package com.microsoft.azure.kusto.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class KustoResultMapperTest {
	
	public static enum TestEnum {
		A, B, C;
	}
	
	public static class TestPojo {
		int id;
		String name;
		ZonedDateTime zonedTime;
		Instant instantTime;
		TestEnum testEnum;
		UUID uuid;
		
		public void setId(int id) {
			this.id = id;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public void setZonedTime(ZonedDateTime time) {
			this.zonedTime = time;
		}
		
		public void setInstantTime(Instant time) {
			this.instantTime = time;
		}
		
		public void setUuid(UUID uuid) {
			this.uuid = uuid;
		}
		
		public void setTestEnum(TestEnum testEnum) {
			this.testEnum = testEnum;
		}
	}
	
	static final KustoResultMapper<TestPojo> namedMapper = KustoResultMapper.newBuilder(TestPojo::new)
			.addNonNullableColumn(KustoType.INTEGER, "Id", TestPojo::setId).addNullableColumn(KustoType.STRING, "Name", TestPojo::setName)
			.addNullableColumn(KustoType.DATETIME_ZONED_DATE_TIME, "TimeColumn", TestPojo::setZonedTime)
			.addNullableColumn(KustoType.GUID_UUID, "Guid", TestPojo::setUuid)
			.addNullableColumn(KustoType.STRING, "Enum", (r, t) -> r.setTestEnum(t != null ? TestEnum.valueOf(t) : null))
			.addNullableColumn(KustoType.DATETIME_INSTANT, "Instant", TestPojo::setInstantTime).build();
	
	static final KustoResultMapper<TestPojo> ordinalMapper = KustoResultMapper.newBuilder(TestPojo::new)
			.addNonNullableColumn(KustoType.INTEGER, 1, TestPojo::setId).addNullableColumn(KustoType.STRING, 2, TestPojo::setName)
			.addNullableColumn(KustoType.DATETIME_ZONED_DATE_TIME, 3, TestPojo::setZonedTime).addNullableColumn(KustoType.GUID_UUID, 4, TestPojo::setUuid)
			.addNullableColumn(KustoType.STRING, "Enum", (r, t) -> r.setTestEnum(t != null ? TestEnum.valueOf(t) : null))
			.addNullableColumn(KustoType.DATETIME_INSTANT, 6, TestPojo::setInstantTime).build();
	
	static final KustoResultMapper<TestPojo> mixedMapper = KustoResultMapper.newBuilder(TestPojo::new)
			.addNonNullableColumn(KustoType.INTEGER, "Id", 1, TestPojo::setId).addNullableColumn(KustoType.STRING, "Name", 2, TestPojo::setName)
			.addNullableColumn(KustoType.DATETIME_ZONED_DATE_TIME, "TimeColumn", 3, TestPojo::setZonedTime)
			.addNullableColumn(KustoType.GUID_UUID, "Guid", 4, TestPojo::setUuid)
			.addNullableColumn(KustoType.STRING, "Enum", 5, (r, t) -> r.setTestEnum(t != null ? TestEnum.valueOf(t) : null))
			.addNullableColumn(KustoType.DATETIME_INSTANT, "Instant", 6, TestPojo::setInstantTime).build();
	
	KustoResultSetTable resultSet = mock(KustoResultSetTable.class);
	
	@BeforeEach
	public void prepareResultSet() {
		when(this.resultSet.findColumn("Id")).thenReturn(1);
		when(this.resultSet.findColumn("Name")).thenReturn(2);
		when(this.resultSet.findColumn("TimeColumn")).thenReturn(3);
		when(this.resultSet.findColumn("Guid")).thenReturn(4);
		when(this.resultSet.findColumn("Enum")).thenReturn(5);
		when(this.resultSet.findColumn("Instant")).thenReturn(6);
	}
	
	void testNonNull(KustoResultMapper<TestPojo> mapper) throws Exception {
		when(this.resultSet.getObject(1)).thenReturn(Integer.valueOf(1));
		when(this.resultSet.getObject(2)).thenReturn("MyName");
		when(this.resultSet.getObject(3)).thenReturn("1970-01-01T00:00:00.001Z");
		when(this.resultSet.getObject(4)).thenReturn("e091cf92-6195-4005-bad5-82af80ff1939");
		when(this.resultSet.getObject(5)).thenReturn("C");
		when(this.resultSet.getObject(6)).thenReturn("1970-01-01T00:00:00.002Z");
		when(this.resultSet.next()).thenReturn(true);
		
		TestPojo pojo = mapper.extractSingle(this.resultSet);
		assertNotNull(pojo);
		assertEquals(1, pojo.id);
		assertEquals("MyName", pojo.name);
		assertEquals(1, pojo.zonedTime.toInstant().toEpochMilli());
		assertEquals(UUID.fromString("e091cf92-6195-4005-bad5-82af80ff1939"), pojo.uuid);
		assertEquals(TestEnum.C, pojo.testEnum);
		assertEquals(2, pojo.instantTime.toEpochMilli());
	}
	
	void testNullable(KustoResultMapper<TestPojo> mapper) throws Exception {
		when(this.resultSet.getObject(1)).thenReturn(Integer.valueOf(1));
		when(this.resultSet.getObject(2)).thenReturn(null);
		when(this.resultSet.getObject(3)).thenReturn(null);
		when(this.resultSet.getObject(4)).thenReturn(null);
		when(this.resultSet.getObject(5)).thenReturn(null);
		when(this.resultSet.getObject(6)).thenReturn(null);
		when(this.resultSet.next()).thenReturn(true);
		
		TestPojo pojo = mapper.extractSingle(this.resultSet);
		assertNotNull(pojo);
		assertEquals(1, pojo.id);
		assertNull(pojo.name);
		assertNull(pojo.zonedTime);
		assertNull(pojo.uuid);
		assertNull(pojo.testEnum);
		assertNull(pojo.instantTime);
	}
	
	void testNullThrowing(KustoResultMapper<TestPojo> mapper) throws Exception {
		when(this.resultSet.getObject(1)).thenReturn(null);
		when(this.resultSet.next()).thenReturn(true);
		
		assertThrows(NullPointerException.class, () -> mapper.extractSingle(this.resultSet));
	}
	
	void testList(KustoResultMapper<TestPojo> mapper) throws Exception {
		when(this.resultSet.getObject(1)).thenReturn(Integer.valueOf(1)).thenReturn(Integer.valueOf(2));
		when(this.resultSet.getObject(2)).thenReturn("MyName").thenReturn("OtherName");
		when(this.resultSet.getObject(3)).thenReturn("1970-01-01T00:00:00.001Z").thenReturn("1970-01-01T00:00:00.003Z");
		when(this.resultSet.getObject(4)).thenReturn("e091cf92-6195-4005-bad5-82af80ff1939").thenReturn("dc90cbef-0d82-4a79-bb34-7e7798bf962b");
		when(this.resultSet.getObject(5)).thenReturn("C").thenReturn("A");
		when(this.resultSet.getObject(6)).thenReturn("1970-01-01T00:00:00.002Z").thenReturn("1970-01-01T00:00:00.004Z");
		when(this.resultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
		
		List<TestPojo> list = mapper.extractList(this.resultSet);
		assertNotNull(list);
		assertEquals(2, list.size());
		
		TestPojo pojo = list.get(0);
		assertNotNull(pojo);
		assertEquals(1, pojo.id);
		assertEquals("MyName", pojo.name);
		assertEquals(1, pojo.zonedTime.toInstant().toEpochMilli());
		assertEquals(UUID.fromString("e091cf92-6195-4005-bad5-82af80ff1939"), pojo.uuid);
		assertEquals(TestEnum.C, pojo.testEnum);
		assertEquals(2, pojo.instantTime.toEpochMilli());
		
		pojo = list.get(1);
		assertNotNull(pojo);
		assertEquals(2, pojo.id);
		assertEquals("OtherName", pojo.name);
		assertEquals(3, pojo.zonedTime.toInstant().toEpochMilli());
		assertEquals(UUID.fromString("dc90cbef-0d82-4a79-bb34-7e7798bf962b"), pojo.uuid);
		assertEquals(TestEnum.A, pojo.testEnum);
		assertEquals(4, pojo.instantTime.toEpochMilli());
	}
	
	void testNoRows(KustoResultMapper<TestPojo> mapper) throws Exception {
		when(this.resultSet.next()).thenReturn(false);
		TestPojo pojo = mapper.extractSingle(this.resultSet);
		assertNull(pojo);
	}
	
	@Test
	void testExtractSingleOrdinalMapperNonNullable() throws Exception {
		testNonNull(ordinalMapper);
	}
	
	@Test
	void testExtractSingleMixedMapperNonNullable() throws Exception {
		testNonNull(mixedMapper);
	}
	
	@Test
	void testExtractSingleNamedMapperNonNullable() throws Exception {
		testNonNull(namedMapper);
	}
	
	@Test
	void testExtractSingleceOrdinalNoRows() throws Exception {
		testNoRows(ordinalMapper);
	}
	
	@Test
	void testExtractSingleMixedMapperNoRows() throws Exception {
		testNoRows(mixedMapper);
	}
	
	@Test
	void testExtractSingleNamedMapperNoRows() throws Exception {
		testNoRows(namedMapper);
	}
	
	@Test
	void testExtractListOrdinalMapperNonNullable() throws Exception {
		testList(ordinalMapper);
	}
	
	@Test
	void testExtractListMixedMapperNonNullable() throws Exception {
		testList(mixedMapper);
	}
	
	@Test
	void testExtractListNamedMapperNonNullable() throws Exception {
		testList(namedMapper);
	}
	
	@Test
	void testExtractListOrdinalMapperNullable() throws Exception {
		testNullable(ordinalMapper);
	}
	
	@Test
	void testExtractListMixedMapperNullable() throws Exception {
		testNullable(mixedMapper);
	}
	
	@Test
	void testExtractListNamedMapperNullable() throws Exception {
		testNullable(namedMapper);
	}
	
	@Test
	void testExtractListOrdinalMapperNullThrowing() throws Exception {
		testNullThrowing(ordinalMapper);
	}
	
	@Test
	void testExtractListMixedMapperNullThrowing() throws Exception {
		testNullThrowing(mixedMapper);
	}
	
	@Test
	void testExtractListNamedMapperNullThrowing() throws Exception {
		testNullThrowing(namedMapper);
	}
}
