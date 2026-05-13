import api from './client';

export interface BannerResponse {
  id: number;
  badge: string;
  title: string;
  subtitle: string;
  ctaText: string;
  ctaPath: string;
  imageUrl: string | null;
  accentColor: string;
  orderIndex: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface BannerRequest {
  badge: string;
  title: string;
  subtitle: string;
  ctaText: string;
  ctaPath: string;
  accentColor: string;
  orderIndex: number;
  isActive: boolean;
}

export const bannerApi = {
  // 공개 - 활성 배너 목록
  getActiveBanners: () =>
    api.get<{ data: BannerResponse[] }>('/api/banners'),

  // 관리자 - 전체 배너 목록
  getAllBanners: () =>
    api.get<{ data: BannerResponse[] }>('/api/banners/admin'),

  // 관리자 - 배너 생성 (이미지 포함)
  createBanner: (data: BannerRequest, image?: File) => {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (image) formData.append('image', image);
    return api.post('/api/banners', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  // 관리자 - 배너 수정 (이미지 포함)
  updateBanner: (bannerId: number, data: BannerRequest, image?: File) => {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (image) formData.append('image', image);
    return api.patch(`/api/banners/${bannerId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  // 관리자 - 배너 삭제
  deleteBanner: (bannerId: number) =>
    api.delete(`/api/banners/${bannerId}`),
};
