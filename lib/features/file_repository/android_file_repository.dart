import 'package:flutter/services.dart';
import 'package:passtable/features/file_repository/abstract_file_repository.dart';

class AndroidFileRepository implements AbstractFileRepository {
  static const MethodChannel _channel = MethodChannel(
    'file_repository_channel',
  );

  @override
  Future writeData({required String filePath, required String data}) async {
    // TODO if filePath is blank
    final bool result = await _channel.invokeMethod('writeData', {
      'uri': filePath,
      'data': data,
    });
    if (!result) {
      throw Exception("writeData"); // TODO
    }
  }

  @override
  Future<String> readData({required String filePath}) async {
    final String? data = await _channel.invokeMethod('readData', {
      'uri': filePath,
    });
    if (data != null) {
      return data;
    } else {
      throw Exception("readData"); // TODO
    }
  }

  @override
  Future<String> createFile({
    required String directoryPath,
    required String fileName,
  }) async {
    final String? fileUriString = await _channel.invokeMethod('createFile', {
      'treeUri': directoryPath,
      'fileName': fileName,
    });
    if (fileUriString != null) {
      return fileUriString;
    } else {
      throw Exception("createFile"); // TODO
    }
  }
}
