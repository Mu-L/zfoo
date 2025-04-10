class ${protocol_name}Registration:
	func write(buffer: ByteBuffer, packet: _${protocol_name}):
		if (packet == null):
			buffer.writeInt(0)
			return
		${protocol_write_serialization}
		pass

	func read(buffer: ByteBuffer) -> _${protocol_name}:
		var length = buffer.readInt()
		if (length == 0):
			return null
		var beforeReadIndex = buffer.getReadOffset()
		var packet: _${protocol_name} = buffer.newInstance(${protocol_id})
		${protocol_read_deserialization}
		if (length > 0):
			buffer.setReadOffset(beforeReadIndex + length)
		return packet