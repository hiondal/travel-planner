import 'package:flutter/material.dart';

/// 앱 공통 텍스트 필드 위젯
/// style-guide.md 6-7 기반
class AppTextField extends StatelessWidget {
  const AppTextField({
    super.key,
    this.controller,
    this.hint,
    this.label,
    this.prefixIcon,
    this.suffixIcon,
    this.onChanged,
    this.onSubmitted,
    this.obscureText = false,
    this.keyboardType,
    this.textInputAction,
    this.errorText,
    this.enabled = true,
    this.maxLines = 1,
    this.autofocus = false,
    this.focusNode,
  });

  final TextEditingController? controller;
  final String? hint;
  final String? label;
  final IconData? prefixIcon;
  final Widget? suffixIcon;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onSubmitted;
  final bool obscureText;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final String? errorText;
  final bool enabled;
  final int maxLines;
  final bool autofocus;
  final FocusNode? focusNode;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      onChanged: onChanged,
      onSubmitted: onSubmitted,
      obscureText: obscureText,
      keyboardType: keyboardType,
      textInputAction: textInputAction,
      enabled: enabled,
      maxLines: maxLines,
      autofocus: autofocus,
      focusNode: focusNode,
      decoration: InputDecoration(
        hintText: hint,
        labelText: label,
        prefixIcon: prefixIcon != null ? Icon(prefixIcon) : null,
        suffixIcon: suffixIcon,
        errorText: errorText,
      ),
    );
  }
}
