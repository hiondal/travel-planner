import 'package:flutter/material.dart';

import '../constants/app_colors.dart';
import '../constants/app_spacing.dart';
import '../constants/app_typography.dart';

/// style-guide.md 3절 ColorScheme.dark() 매핑 기반 다크 테마
/// Material Design 3 적용
abstract final class AppTheme {
  AppTheme._();

  /// 앱 전체 다크 테마
  static ThemeData get darkTheme {
    final colorScheme = _buildColorScheme();

    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      colorScheme: colorScheme,
      fontFamily: 'Pretendard',
      textTheme: _buildTextTheme(colorScheme),
      appBarTheme: _buildAppBarTheme(colorScheme),
      bottomNavigationBarTheme: _buildBottomNavBarTheme(colorScheme),
      navigationBarTheme: _buildNavigationBarTheme(colorScheme),
      cardTheme: _buildCardTheme(),
      elevatedButtonTheme: _buildElevatedButtonTheme(colorScheme),
      outlinedButtonTheme: _buildOutlinedButtonTheme(colorScheme),
      textButtonTheme: _buildTextButtonTheme(colorScheme),
      inputDecorationTheme: _buildInputDecorationTheme(colorScheme),
      bottomSheetTheme: _buildBottomSheetTheme(colorScheme),
      chipTheme: _buildChipTheme(colorScheme),
      dividerTheme: const DividerThemeData(
        color: AppColors.outline,
        thickness: 1,
        space: 0,
      ),
      snackBarTheme: _buildSnackBarTheme(),
      scaffoldBackgroundColor: AppColors.bgPrimary,
    );
  }

  // ---------------------------------------------------------------------------
  // ColorScheme — style-guide.md 3-6 기반
  // ---------------------------------------------------------------------------

  static ColorScheme _buildColorScheme() {
    return const ColorScheme.dark(
      brightness: Brightness.dark,
      primary: AppColors.accentRed,           // #FF2D2D
      onPrimary: AppColors.textPrimary,        // #F0F0F0
      secondary: AppColors.accentPurple,       // #7B3FE0
      onSecondary: AppColors.textPrimary,      // #F0F0F0
      tertiary: AppColors.accentBlue,          // #0A84FF
      onTertiary: AppColors.textPrimary,       // #F0F0F0
      surface: AppColors.bgCard,               // #1A1A1A
      onSurface: AppColors.textPrimary,        // #F0F0F0
      surfaceContainerHighest: AppColors.bgInput, // #242424 (surfaceVariant 대체)
      onSurfaceVariant: AppColors.textSecondary,  // #8A8A8A
      error: AppColors.statusRed,             // #FF3B30
      onError: AppColors.textPrimary,          // #F0F0F0
      outline: AppColors.outline,              // #2A2A2A
      shadow: Colors.black,
      inverseSurface: AppColors.textPrimary,   // #F0F0F0 (토스트 배경)
      onInverseSurface: AppColors.textInverse, // #0A0A0A (토스트 텍스트)
      scrim: Colors.black,
    );
  }

  // ---------------------------------------------------------------------------
  // TextTheme — style-guide.md 4-2 기반
  // ---------------------------------------------------------------------------

  static TextTheme _buildTextTheme(ColorScheme colorScheme) {
    return TextTheme(
      displayLarge: AppTypography.displayLarge.copyWith(
        color: colorScheme.onSurface,
      ),
      displayMedium: AppTypography.displayMedium.copyWith(
        color: colorScheme.onSurface,
      ),
      displaySmall: AppTypography.displaySmall.copyWith(
        color: colorScheme.onSurface,
      ),
      headlineMedium: AppTypography.headlineMedium.copyWith(
        color: colorScheme.onSurface,
      ),
      headlineSmall: AppTypography.headlineSmall.copyWith(
        color: colorScheme.onSurface,
      ),
      titleLarge: AppTypography.titleLarge.copyWith(
        color: colorScheme.onSurface,
      ),
      titleMedium: AppTypography.titleMedium.copyWith(
        color: colorScheme.onSurfaceVariant,
      ),
      titleSmall: AppTypography.titleSmall.copyWith(
        color: colorScheme.onSurfaceVariant,
      ),
      bodyLarge: AppTypography.bodyLarge.copyWith(
        color: colorScheme.onSurface,
      ),
      bodyMedium: AppTypography.bodyMedium.copyWith(
        color: colorScheme.onSurface,
      ),
      bodySmall: AppTypography.bodySmall.copyWith(
        color: colorScheme.onSurfaceVariant,
      ),
      labelLarge: AppTypography.labelLarge.copyWith(
        color: colorScheme.onSurface,
      ),
      labelMedium: AppTypography.labelMedium.copyWith(
        color: colorScheme.onSurface,
      ),
      labelSmall: AppTypography.labelSmall.copyWith(
        color: colorScheme.onSurfaceVariant,
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // AppBar — style-guide.md 6-4 기반
  // ---------------------------------------------------------------------------

  static AppBarTheme _buildAppBarTheme(ColorScheme colorScheme) {
    return AppBarTheme(
      backgroundColor: AppColors.bgPrimary,
      foregroundColor: AppColors.textPrimary,
      elevation: 0,
      centerTitle: true,
      toolbarHeight: AppSpacing.appBarHeight,
      titleTextStyle: AppTypography.displaySmall.copyWith(
        color: AppColors.textPrimary,
      ),
      iconTheme: const IconThemeData(color: AppColors.textPrimary),
      actionsIconTheme: const IconThemeData(color: AppColors.textPrimary),
    );
  }

  // ---------------------------------------------------------------------------
  // BottomNavigationBar (레거시) — style-guide.md 6-5 기반
  // ---------------------------------------------------------------------------

  static BottomNavigationBarThemeData _buildBottomNavBarTheme(
    ColorScheme colorScheme,
  ) {
    return const BottomNavigationBarThemeData(
      backgroundColor: AppColors.bgCard,
      selectedItemColor: AppColors.textPrimary,
      unselectedItemColor: AppColors.textSecondary,
      type: BottomNavigationBarType.fixed,
      selectedLabelStyle: AppTypography.labelSmall,
      unselectedLabelStyle: AppTypography.labelSmall,
      elevation: 0,
    );
  }

  // ---------------------------------------------------------------------------
  // NavigationBar (M3) — style-guide.md 6-5 기반
  // ---------------------------------------------------------------------------

  static NavigationBarThemeData _buildNavigationBarTheme(
    ColorScheme colorScheme,
  ) {
    return NavigationBarThemeData(
      backgroundColor: AppColors.bgCard,
      indicatorColor: Colors.transparent,
      iconTheme: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.selected)) {
          return const IconThemeData(color: AppColors.textPrimary, size: 24);
        }
        return const IconThemeData(color: AppColors.textSecondary, size: 24);
      }),
      labelTextStyle: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.selected)) {
          return AppTypography.labelSmall.copyWith(
            color: AppColors.textPrimary,
          );
        }
        return AppTypography.labelSmall.copyWith(
          color: AppColors.textSecondary,
        );
      }),
      height: AppSpacing.bottomNavBarHeight,
      surfaceTintColor: Colors.transparent,
    );
  }

  // ---------------------------------------------------------------------------
  // Card — style-guide.md 6-2 기반
  // ---------------------------------------------------------------------------

  static CardTheme _buildCardTheme() {
    return CardTheme(
      color: AppColors.bgCard,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppSpacing.radiusCard),
      ),
      clipBehavior: Clip.antiAlias,
      margin: EdgeInsets.zero,
    );
  }

  // ---------------------------------------------------------------------------
  // ElevatedButton — style-guide.md 6-1 기반
  // ---------------------------------------------------------------------------

  static ElevatedButtonThemeData _buildElevatedButtonTheme(
    ColorScheme colorScheme,
  ) {
    return ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: AppColors.accentRed,
        foregroundColor: AppColors.textPrimary,
        disabledBackgroundColor: AppColors.textDisabled,
        disabledForegroundColor: AppColors.textSecondary,
        minimumSize: const Size(120, AppSpacing.buttonHeight),
        padding: const EdgeInsets.symmetric(horizontal: 24),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
        ),
        textStyle: AppTypography.labelLarge,
        elevation: 0,
        splashFactory: InkRipple.splashFactory,
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // OutlinedButton — style-guide.md 6-1 기반
  // ---------------------------------------------------------------------------

  static OutlinedButtonThemeData _buildOutlinedButtonTheme(
    ColorScheme colorScheme,
  ) {
    return OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: AppColors.accentRed,
        side: const BorderSide(color: AppColors.accentRed, width: 1.5),
        minimumSize: const Size(120, AppSpacing.buttonHeight),
        padding: const EdgeInsets.symmetric(horizontal: 24),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
        ),
        textStyle: AppTypography.labelMedium,
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // TextButton — style-guide.md 6-1 기반
  // ---------------------------------------------------------------------------

  static TextButtonThemeData _buildTextButtonTheme(
    ColorScheme colorScheme,
  ) {
    return TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: AppColors.accentBlue,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        textStyle: AppTypography.labelMedium,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
        ),
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // InputDecoration — style-guide.md 6-7 기반
  // ---------------------------------------------------------------------------

  static InputDecorationTheme _buildInputDecorationTheme(
    ColorScheme colorScheme,
  ) {
    return InputDecorationTheme(
      filled: true,
      fillColor: AppColors.bgInput,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
        borderSide: BorderSide.none,
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
        borderSide: BorderSide.none,
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
        borderSide: const BorderSide(color: AppColors.accentBlue, width: 1.5),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
        borderSide: const BorderSide(color: AppColors.statusRed, width: 1.5),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(AppSpacing.radiusButton),
        borderSide: const BorderSide(color: AppColors.statusRed, width: 1.5),
      ),
      contentPadding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.spaceBase,
        vertical: 14,
      ),
      hintStyle: AppTypography.bodyMedium.copyWith(
        color: AppColors.textSecondary,
      ),
      labelStyle: AppTypography.bodyMedium.copyWith(
        color: AppColors.textSecondary,
      ),
      prefixIconColor: AppColors.textSecondary,
      suffixIconColor: AppColors.textSecondary,
    );
  }

  // ---------------------------------------------------------------------------
  // BottomSheet — style-guide.md 6-3 기반
  // ---------------------------------------------------------------------------

  static BottomSheetThemeData _buildBottomSheetTheme(
    ColorScheme colorScheme,
  ) {
    return BottomSheetThemeData(
      backgroundColor: AppColors.bgHover,   // #2A2A2A (Overlay 레벨)
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppSpacing.radiusBottomSheet),
        ),
      ),
      showDragHandle: false, // 커스텀 핸들 사용
      modalBarrierColor: AppColors.bgOverlay,
      dragHandleColor: AppColors.textDisabled,
      dragHandleSize: Size(
        AppSpacing.bottomSheetHandleWidth,
        AppSpacing.bottomSheetHandleHeight,
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // Chip — style-guide.md 6-9 기반
  // ---------------------------------------------------------------------------

  static ChipThemeData _buildChipTheme(ColorScheme colorScheme) {
    return ChipThemeData(
      backgroundColor: AppColors.bgInput,
      selectedColor: AppColors.accentRed.withOpacity(0.2),
      labelStyle: AppTypography.labelSmall.copyWith(
        color: AppColors.textPrimary,
      ),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppSpacing.radiusChip),
      ),
      padding: const EdgeInsets.symmetric(
        horizontal: 10,
        vertical: 4,
      ),
      side: BorderSide.none,
    );
  }

  // ---------------------------------------------------------------------------
  // SnackBar — style-guide.md 9-9 기반
  // ---------------------------------------------------------------------------

  static SnackBarThemeData _buildSnackBarTheme() {
    return SnackBarThemeData(
      backgroundColor: AppColors.elevationTop,   // #333333
      contentTextStyle: AppTypography.bodyMedium.copyWith(
        color: AppColors.textPrimary,
      ),
      actionTextColor: AppColors.accentBlue,
      behavior: SnackBarBehavior.floating,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppSpacing.radiusSnackBar),
      ),
    );
  }
}
