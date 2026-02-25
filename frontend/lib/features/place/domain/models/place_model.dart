/// 장소 검색 결과 모델 (PLCE 서비스)
class Place {
  const Place({
    required this.placeId,
    required this.name,
    required this.category,
    required this.address,
    required this.lat,
    required this.lng,
    required this.rating,
    required this.reviewCount,
    this.imageUrl,
    this.openNow,
    this.businessHours,
    this.phoneNumber,
    this.priceLevel,
  });

  final String placeId;
  final String name;
  final String category;
  final String address;
  final double lat;
  final double lng;
  final double rating;
  final int reviewCount;
  final String? imageUrl;
  final bool? openNow;
  final List<String>? businessHours;
  final String? phoneNumber;
  final int? priceLevel;

  factory Place.fromJson(Map<String, dynamic> json) {
    // 백엔드 응답: coordinates 객체 {lat, lng} 또는 flat lat/lng 모두 지원
    double lat = 0;
    double lng = 0;
    if (json['coordinates'] is Map<String, dynamic>) {
      final coords = json['coordinates'] as Map<String, dynamic>;
      lat = (coords['lat'] as num? ?? 0).toDouble();
      lng = (coords['lng'] as num? ?? 0).toDouble();
    } else {
      lat = (json['lat'] as num? ?? 0).toDouble();
      lng = (json['lng'] as num? ?? 0).toDouble();
    }

    // 백엔드 응답: business_hours가 객체 배열 [{day, open, close}] 또는 문자열 배열
    List<String>? businessHours;
    final rawHours = json['business_hours'] as List<dynamic>?;
    if (rawHours != null && rawHours.isNotEmpty) {
      if (rawHours.first is String) {
        businessHours = rawHours.map((e) => e as String).toList();
      } else if (rawHours.first is Map) {
        businessHours = rawHours.map((e) {
          final m = e as Map<String, dynamic>;
          return '${m['day']}: ${m['open']}-${m['close']}';
        }).toList();
      }
    }

    return Place(
      placeId: json['place_id'] as String,
      name: json['name'] as String,
      category: json['category'] as String? ?? '',
      address: json['address'] as String? ?? '',
      lat: lat,
      lng: lng,
      rating: (json['rating'] as num? ?? 0).toDouble(),
      reviewCount: json['review_count'] as int? ?? 0,
      imageUrl: json['image_url'] as String?,
      openNow: json['open_now'] as bool?,
      businessHours: businessHours,
      phoneNumber: json['phone_number'] as String?,
      priceLevel: json['price_level'] as int?,
    );
  }
}

/// 장소 검색 응답
class PlaceSearchResponse {
  const PlaceSearchResponse({required this.places});

  final List<Place> places;

  factory PlaceSearchResponse.fromJson(Map<String, dynamic> json) {
    final list = json['places'] as List<dynamic>? ?? [];
    return PlaceSearchResponse(
      places: list
          .map((e) => Place.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}
