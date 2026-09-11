package in.sb.pinac.service.admin;

import in.sb.pinac.dto.admin.WebsiteSettingDto;
import in.sb.pinac.entity.WebsiteSetting;
import in.sb.pinac.repository.WebsiteSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSettingService {

    @Autowired
    private WebsiteSettingRepository websiteSettingRepository;

    public WebsiteSetting getSettings() {
        return websiteSettingRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    WebsiteSetting defaultSetting = new WebsiteSetting();
                    return websiteSettingRepository.save(defaultSetting);
                });
    }

    @Transactional
    public WebsiteSetting updateSettings(WebsiteSettingDto dto) {
        WebsiteSetting setting = getSettings();

        if (dto.getWebsiteName() != null) setting.setWebsiteName(dto.getWebsiteName());
        if (dto.getTagline() != null) setting.setTagline(dto.getTagline());
        if (dto.getLogoUrl() != null) setting.setLogoUrl(dto.getLogoUrl());
        if (dto.getFaviconUrl() != null) setting.setFaviconUrl(dto.getFaviconUrl());
        if (dto.getContactEmail() != null) setting.setContactEmail(dto.getContactEmail());
        if (dto.getContactPhone() != null) setting.setContactPhone(dto.getContactPhone());
        if (dto.getAddress() != null) setting.setAddress(dto.getAddress());
        if (dto.getFacebookUrl() != null) setting.setFacebookUrl(dto.getFacebookUrl());
        if (dto.getInstagramUrl() != null) setting.setInstagramUrl(dto.getInstagramUrl());
        if (dto.getYoutubeUrl() != null) setting.setYoutubeUrl(dto.getYoutubeUrl());
        if (dto.getLinkedinUrl() != null) setting.setLinkedinUrl(dto.getLinkedinUrl());
        if (dto.getTwitterUrl() != null) setting.setTwitterUrl(dto.getTwitterUrl());
        if (dto.getRazorpayKeyId() != null) setting.setRazorpayKeyId(dto.getRazorpayKeyId());
        if (dto.getRazorpayKeySecret() != null) setting.setRazorpayKeySecret(dto.getRazorpayKeySecret());
        if (dto.getSmtpHost() != null) setting.setSmtpHost(dto.getSmtpHost());
        if (dto.getSmtpPort() != null) setting.setSmtpPort(dto.getSmtpPort());
        if (dto.getSmtpUsername() != null) setting.setSmtpUsername(dto.getSmtpUsername());
        if (dto.getSmtpPassword() != null && !dto.getSmtpPassword().trim().isEmpty()) setting.setSmtpPassword(dto.getSmtpPassword());
        if (dto.getSmtpFromEmail() != null) setting.setSmtpFromEmail(dto.getSmtpFromEmail());
        if (dto.getMetaTitle() != null) setting.setMetaTitle(dto.getMetaTitle());
        if (dto.getMetaDescription() != null) setting.setMetaDescription(dto.getMetaDescription());
        if (dto.getMetaKeywords() != null) setting.setMetaKeywords(dto.getMetaKeywords());

        return websiteSettingRepository.save(setting);
    }
}
