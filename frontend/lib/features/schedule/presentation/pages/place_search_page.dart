import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_spacing.dart';
import '../../../../core/constants/app_typography.dart';
import '../../../../core/routing/app_routes.dart';
import '../../../../shared/widgets/app_bottom_sheet.dart';
import '../../../../shared/widgets/app_skeleton.dart';
import '../../../place/domain/models/place_model.dart';
import '../../../place/presentation/providers/place_provider.dart';

/// 장소 검색 화면 (SCR-014)
/// UFR-SCHD-020, UFR-PLCE-010: 장소 검색 및 일정 추가
class PlaceSearchPage extends ConsumerStatefulWidget {
  const PlaceSearchPage({super.key, required this.tripId});

  final String tripId;

  @override
  ConsumerState<PlaceSearchPage> createState() => _PlaceSearchPageState();
}

class _PlaceSearchPageState extends ConsumerState<PlaceSearchPage> {
  final _searchController = TextEditingController();
  final _focusNode = FocusNode();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _focusNode.requestFocus();
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  void _onSearch(String keyword) {
    ref.read(placeSearchProvider.notifier).search(keyword: keyword);
  }

  void _onPlaceTap(Place place) {
    context.goNamed(
      AppRoutes.placeTimePickerName,
      pathParameters: {'tripId': widget.tripId},
      queryParameters: {'placeId': place.placeId},
    );
  }

  void _showPlaceDetail(Place place) {
    AppBottomSheet.show(
      context,
      child: _PlaceDetailSheet(
        place: place,
        onAddToSchedule: () {
          Navigator.of(context).pop();
          _onPlaceTap(place);
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final searchState = ref.watch(placeSearchProvider);

    return Scaffold(
      appBar: AppBar(
        title: TextField(
          controller: _searchController,
          focusNode: _focusNode,
          onChanged: _onSearch,
          style: AppTypography.bodyLarge.copyWith(
            color: AppColors.textPrimary,
          ),
          decoration: InputDecoration(
            hintText: '장소 이름으로 검색',
            hintStyle: AppTypography.bodyLarge.copyWith(
              color: AppColors.textSecondary,
            ),
            border: InputBorder.none,
            filled: false,
            prefixIcon: const Icon(
              Icons.search,
              color: AppColors.textSecondary,
            ),
            suffixIcon: _searchController.text.isNotEmpty
                ? IconButton(
                    icon: const Icon(
                      Icons.clear,
                      color: AppColors.textSecondary,
                    ),
                    onPressed: () {
                      _searchController.clear();
                      ref.read(placeSearchProvider.notifier).clear();
                    },
                  )
                : null,
          ),
        ),
      ),
      body: searchState.when(
        loading: () => ListView.builder(
          padding: const EdgeInsets.all(AppSpacing.spaceBase),
          itemCount: 6,
          itemBuilder: (_, __) => Padding(
            padding: const EdgeInsets.only(bottom: AppSpacing.spaceSm),
            child: _PlaceSearchItemSkeleton(),
          ),
        ),
        error: (err, _) => Center(
          child: Text(
            '검색 중 오류가 발생했습니다.',
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ),
        data: (places) => places.isEmpty
            ? _searchController.text.isEmpty
                ? _SearchHintView()
                : _EmptySearchResult(keyword: _searchController.text)
            : ListView.separated(
                padding: const EdgeInsets.all(AppSpacing.spaceBase),
                itemCount: places.length,
                separatorBuilder: (_, __) => const Divider(height: 1),
                itemBuilder: (context, index) {
                  final place = places[index];
                  return _PlaceSearchResultTile(
                    place: place,
                    onTap: () => _onPlaceTap(place),
                    onDetailTap: () => _showPlaceDetail(place),
                  );
                },
              ),
      ),
    );
  }
}

class _PlaceSearchResultTile extends StatelessWidget {
  const _PlaceSearchResultTile({
    required this.place,
    required this.onTap,
    required this.onDetailTap,
  });

  final Place place;
  final VoidCallback onTap;
  final VoidCallback onDetailTap;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.spaceBase,
        vertical: AppSpacing.spaceXs,
      ),
      leading: Container(
        width: 44,
        height: 44,
        decoration: BoxDecoration(
          color: AppColors.bgInput,
          borderRadius: BorderRadius.circular(AppSpacing.spaceSm),
        ),
        child: const Icon(Icons.place, color: AppColors.textSecondary),
      ),
      title: Text(
        place.name,
        style: AppTypography.bodyLarge.copyWith(color: AppColors.textPrimary),
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      subtitle: Text(
        place.address,
        style: AppTypography.bodySmall.copyWith(color: AppColors.textSecondary),
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
      ),
      trailing: IconButton(
        icon: const Icon(
          Icons.info_outline,
          color: AppColors.textSecondary,
          size: 20,
        ),
        onPressed: onDetailTap,
      ),
      onTap: onTap,
    );
  }
}

class _PlaceDetailSheet extends StatelessWidget {
  const _PlaceDetailSheet({
    required this.place,
    required this.onAddToSchedule,
  });

  final Place place;
  final VoidCallback onAddToSchedule;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(AppSpacing.spaceBase),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            place.name,
            style: AppTypography.displaySmall.copyWith(
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: AppSpacing.spaceSm),
          Row(
            children: [
              const Icon(
                Icons.star,
                size: 16,
                color: AppColors.accentAmber,
              ),
              const SizedBox(width: 4),
              Text(
                '${place.rating} (${place.reviewCount})',
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
              const SizedBox(width: AppSpacing.spaceSm),
              if (place.openNow != null)
                Text(
                  place.openNow! ? '영업 중' : '영업 종료',
                  style: AppTypography.bodySmall.copyWith(
                    color: place.openNow!
                        ? AppColors.statusGreen
                        : AppColors.statusRed,
                  ),
                ),
            ],
          ),
          const SizedBox(height: AppSpacing.spaceBase),
          Row(
            children: [
              const Icon(
                Icons.location_on_outlined,
                size: 16,
                color: AppColors.textSecondary,
              ),
              const SizedBox(width: 4),
              Expanded(
                child: Text(
                  place.address,
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.space2xl),
          SizedBox(
            width: double.infinity,
            height: AppSpacing.buttonHeight,
            child: ElevatedButton(
              onPressed: onAddToSchedule,
              child: const Text('일정에 추가'),
            ),
          ),
          SizedBox(height: MediaQuery.of(context).padding.bottom + 8),
        ],
      ),
    );
  }
}

class _PlaceSearchItemSkeleton extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        AppSkeleton(width: 44, height: 44, borderRadius: 8),
        const SizedBox(width: AppSpacing.spaceMd),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              AppSkeleton(width: 140, height: 15),
              const SizedBox(height: 6),
              AppSkeleton(width: 200, height: 12),
            ],
          ),
        ),
      ],
    );
  }
}

class _SearchHintView extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.search, size: 48, color: AppColors.textDisabled),
          const SizedBox(height: AppSpacing.spaceBase),
          Text(
            '장소 이름을 입력하여 검색하세요.',
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }
}

class _EmptySearchResult extends StatelessWidget {
  const _EmptySearchResult({required this.keyword});

  final String keyword;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(
            Icons.search_off,
            size: 48,
            color: AppColors.textDisabled,
          ),
          const SizedBox(height: AppSpacing.spaceBase),
          Text(
            '"$keyword" 검색 결과가 없습니다.',
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }
}
