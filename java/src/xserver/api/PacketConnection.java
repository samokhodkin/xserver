package xserver.api;

import java.nio.ByteBuffer;

/*
 * ���������, ����������� ����������� � ���� � �������� ��������� ������.
 * ������� �� �� ��� � � StreamConnection, �� ��������������� ��� ������ 
 * �������� � ���������� ���� (�� ������� � �� ���������).
 * 
 * ��������� ���������� ����� ���������� � �������� � ������� ���������� xserver.io.Packetizer
 */

public interface PacketConnection<DataType> extends StreamConnection<DataType>{}
