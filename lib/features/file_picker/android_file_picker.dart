import 'package:flutter/services.dart';
import 'package:passtable/features/file_picker/abstract_file_picker.dart';

class AndroidFilePicker implements AbstractFilePicker {
  static const MethodChannel _channel = MethodChannel('file_picker_channel');

  @override
  Future<String?> pickFile() async {
    try {
      final String? uriString = await _channel.invokeMethod('pickFile');
      return uriString;
    } on PlatformException catch (e) {
      // TODO
      //throw;
      return null;
    }
  }

  @override
  Future<String?> pickDirectory() async {
    try {
      final String? uriString = await _channel.invokeMethod('pickDirectory');
      return uriString;
    } on PlatformException catch (e) {
      // TODO
      //throw;
      return null;
    }
  }
}
