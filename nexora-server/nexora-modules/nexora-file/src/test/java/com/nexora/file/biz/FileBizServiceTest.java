package com.nexora.file.biz;

import com.nexora.constants.SecurityConstants;
import com.nexora.file.constants.FileConstants;
import com.nexora.contract.StoredFileUsageChecker;
import com.nexora.file.domain.form.OssFileQueryForm;
import com.nexora.file.domain.query.OssFileQuery;
import com.nexora.file.entity.SysOssFile;
import com.nexora.file.service.SysOssFileService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.oss.model.OssUploadResult;
import com.aurora.starter.oss.template.OssTemplate;
import com.aurora.starter.security.context.SecurityUtils;
import com.nexora.file.infrastructure.FileUploadValidator;
import com.nexora.file.task.OssFileRecordRetryTask;
import com.aurora.starter.webmvc.exception.BizException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.x.file.storage.core.FileInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileBizServiceTest {

    @Mock
    private FileUploadValidator fileUploadValidator;

    @Mock
    private OssTemplate ossTemplate;

    @Mock
    private SysOssFileService ossFileService;

    @Mock
    private StoredFileUsageChecker sysUserService;

    @Mock
    private OssFileRecordRetryTask retryTask;

    @BeforeEach
    void setUp() {
        lenient().when(fileUploadValidator.validate(any(MultipartFile.class))).thenReturn("image/png");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allowedUploads")
    void acceptsAllowedExtensionsWhenTheDetectedContentTypeMatches(String filename,
                                                                    String expectedContentType,
                                                                    byte[] content) {
        lenient().when(fileUploadValidator.validate(any(MultipartFile.class))).thenReturn(expectedContentType);
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "application/octet-stream", content);
        when(ossTemplate.upload(any(MultipartFile.class), anyString()))
                .thenReturn(uploadResult("file-123"));
        when(ossFileService.saveIfAbsent(any())).thenReturn(true);
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            assertThat(service.upload(file)).isEqualTo("https://oss.example.com/file.png");
        }

        ArgumentCaptor<MultipartFile> uploadCaptor = ArgumentCaptor.forClass(MultipartFile.class);
        verify(ossTemplate).upload(uploadCaptor.capture(), anyString());
        assertThat(uploadCaptor.getValue().getContentType()).isEqualTo(expectedContentType);

        ArgumentCaptor<SysOssFile> recordCaptor = ArgumentCaptor.forClass(SysOssFile.class);
        verify(ossFileService).saveIfAbsent(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getContentType()).isEqualTo(expectedContentType);
    }

    @Test
    void replacesAnUntrustedClientContentTypeForOssAndTheDatabaseRecord() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "text/html", pngBytes());
        OssUploadResult uploadResult = uploadResult("file-123");
        uploadResult.setContentType("text/html");
        when(ossTemplate.upload(any(MultipartFile.class), anyString())).thenReturn(uploadResult);
        when(ossFileService.saveIfAbsent(any())).thenReturn(true);
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            service.upload(file);
        }

        ArgumentCaptor<MultipartFile> uploadCaptor = ArgumentCaptor.forClass(MultipartFile.class);
        verify(ossTemplate).upload(uploadCaptor.capture(), anyString());
        assertThat(uploadCaptor.getValue().getContentType()).isEqualTo("image/png");

        ArgumentCaptor<SysOssFile> recordCaptor = ArgumentCaptor.forClass(SysOssFile.class);
        verify(ossFileService).saveIfAbsent(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getContentType()).isEqualTo("image/png");
    }

    @Test
    void rejectsAnUnsupportedExtensionBeforeCallingOss() {
        lenient().when(fileUploadValidator.validate(any(MultipartFile.class)))
                .thenThrow(new BizException(FileConstants.FILE_EXTENSION_NOT_ALLOWED_MESSAGE));
        MockMultipartFile file = multipartFile("avatar.bmp", pngBytes());
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        assertThatThrownBy(() -> service.upload(file))
                .hasMessage(FileConstants.FILE_EXTENSION_NOT_ALLOWED_MESSAGE);

        verify(ossTemplate, never()).upload(any(MultipartFile.class), anyString());
    }

    @Test
    void rejectsContentDisguisedWithAnAllowedExtensionBeforeCallingOss() {
        lenient().when(fileUploadValidator.validate(any(MultipartFile.class)))
                .thenThrow(new BizException(FileConstants.FILE_CONTENT_TYPE_MISMATCH_MESSAGE));
        MockMultipartFile file = multipartFile("document.png", pdfBytes());
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        assertThatThrownBy(() -> service.upload(file))
                .hasMessage(FileConstants.FILE_CONTENT_TYPE_MISMATCH_MESSAGE);

        verify(ossTemplate, never()).upload(any(MultipartFile.class), anyString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonMp4FtypBrands")
    void rejectsOtherIsoBaseMediaFormatsRenamedAsMp4(String majorBrand) {
        lenient().when(fileUploadValidator.validate(any(MultipartFile.class)))
                .thenThrow(new BizException(FileConstants.FILE_CONTENT_TYPE_MISMATCH_MESSAGE));
        MockMultipartFile file = multipartFile("video.mp4", ftypBytes(majorBrand));
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        assertThatThrownBy(() -> service.upload(file))
                .hasMessage(FileConstants.FILE_CONTENT_TYPE_MISMATCH_MESSAGE);

        verify(ossTemplate, never()).upload(any(MultipartFile.class), anyString());
    }

    @Test
    void rejectsAFileLargerThanFiftyMegabytesBeforeCallingOss() {
        lenient().when(fileUploadValidator.validate(any(MultipartFile.class)))
                .thenThrow(new BizException(FileConstants.FILE_TOO_LARGE_MESSAGE));
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(FileConstants.FILE_UPLOAD_MAX_SIZE + 1);
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        assertThatThrownBy(() -> service.upload(file))
                .hasMessage(FileConstants.FILE_TOO_LARGE_MESSAGE);

        verify(ossTemplate, never()).upload(any(MultipartFile.class), anyString());
    }

    @Test
    void allowsAFileExactlyAtTheSizeLimit() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(FileConstants.FILE_UPLOAD_MAX_SIZE);
        when(file.getOriginalFilename()).thenReturn("avatar.png");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(pngBytes()));
        when(ossTemplate.upload(any(MultipartFile.class), anyString())).thenReturn(uploadResult("file-123"));
        when(ossFileService.saveIfAbsent(any())).thenReturn(true);
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            service.upload(file);
        }

        verify(ossTemplate).upload(any(MultipartFile.class), anyString());
    }

    @Test
    void rejectsABlankOriginalFilenameBeforeCallingOss() {
        lenient().when(fileUploadValidator.validate(any(MultipartFile.class)))
                .thenThrow(new BizException(FileConstants.FILE_NAME_REQUIRED_MESSAGE));
        MockMultipartFile file = multipartFile(" ", pngBytes());
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        assertThatThrownBy(() -> service.upload(file))
                .hasMessage(FileConstants.FILE_NAME_REQUIRED_MESSAGE);

        verify(ossTemplate, never()).upload(any(MultipartFile.class), anyString());
    }

    @Test
    void allowsAnOriginalFilenameWithExactlyTwoHundredFiftyFiveCharacters() {
        String filename = "a".repeat(251) + ".png";
        MockMultipartFile file = multipartFile(filename, pngBytes());
        when(ossTemplate.upload(any(MultipartFile.class), anyString())).thenReturn(uploadResult("file-123"));
        when(ossFileService.saveIfAbsent(any())).thenReturn(true);
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            service.upload(file);
        }

        assertThat(filename).hasSize(FileConstants.FILE_ORIGINAL_FILENAME_MAX_LENGTH);
        verify(ossTemplate).upload(any(MultipartFile.class), anyString());
    }

    @Test
    void rejectsAnOriginalFilenameLongerThanTwoHundredFiftyFiveCharacters() {
        lenient().when(fileUploadValidator.validate(any(MultipartFile.class)))
                .thenThrow(new BizException(FileConstants.FILE_NAME_TOO_LONG_MESSAGE));
        String filename = "a".repeat(252) + ".png";
        MockMultipartFile file = multipartFile(filename, pngBytes());
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        assertThatThrownBy(() -> service.upload(file))
                .hasMessage(FileConstants.FILE_NAME_TOO_LONG_MESSAGE);

        assertThat(filename).hasSize(FileConstants.FILE_ORIGINAL_FILENAME_MAX_LENGTH + 1);
        verify(ossTemplate, never()).upload(any(MultipartFile.class), anyString());
    }

    @Test
    void recordsTheAuthenticatedUploaderAndPreservesTheNativeFileId() {
        MockMultipartFile file = file();
        OssUploadResult uploadResult = uploadResult("native-id");
        when(ossTemplate.upload(any(MultipartFile.class), anyString())).thenReturn(uploadResult);
        when(ossFileService.saveIfAbsent(any())).thenReturn(true);
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        String url;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);
            url = service.upload(file);
        }

        ArgumentCaptor<SysOssFile> captor = ArgumentCaptor.forClass(SysOssFile.class);
        verify(ossFileService).saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getFileId()).isEqualTo("native-id");
        assertThat(captor.getValue().getUploaderId()).isEqualTo(10L);
        assertThat(url).isEqualTo(uploadResult.getUrl());
        verify(retryTask, never()).submit(any());
    }

    @Test
    void generatesAFileIdWhenTheNativeResultHasNone() {
        MockMultipartFile file = file();
        OssUploadResult uploadResult = uploadResult(null);
        when(ossTemplate.upload(any(MultipartFile.class), anyString())).thenReturn(uploadResult);
        when(ossFileService.saveIfAbsent(any())).thenReturn(true);
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);
            service.upload(file);
        }

        ArgumentCaptor<SysOssFile> captor = ArgumentCaptor.forClass(SysOssFile.class);
        verify(ossFileService).saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getFileId()).isNotBlank();
    }

    @Test
    void rejectsUploadBeforeCallingOssWhenTheCurrentUserIsMissing() {
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(0);

            assertThatThrownBy(() -> service.upload(file()))
                    .hasMessage(FileConstants.FILE_CURRENT_USER_REQUIRED_MESSAGE);
        }

        verify(ossTemplate, never()).upload(any(MockMultipartFile.class), anyString());
        verify(ossFileService, never()).saveIfAbsent(any());
    }

    @Test
    void enqueuesTheRecordWhenTheInitialInsertThrows() {
        MockMultipartFile file = file();
        when(ossTemplate.upload(any(MultipartFile.class), anyString())).thenReturn(uploadResult("file-123"));
        when(ossFileService.saveIfAbsent(any())).thenThrow(new IllegalStateException("database unavailable"));
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        String url;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);
            url = service.upload(file);
        }

        assertThat(url).isEqualTo("https://oss.example.com/file.png");
        verify(retryTask).submit(any(SysOssFile.class));
    }

    @Test
    void doesNotRetryTheProducerCallWhenQueueSubmissionFails() {
        MockMultipartFile file = file();
        when(ossTemplate.upload(any(MultipartFile.class), anyString())).thenReturn(uploadResult("file-123"));
        when(ossFileService.saveIfAbsent(any())).thenReturn(false);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(retryTask).submit(any(SysOssFile.class));
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            assertThatThrownBy(() -> service.upload(file))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("redis unavailable");
        }

        verify(retryTask).submit(any(SysOssFile.class));
    }

    @Test
    void forcesTheCurrentUploaderOnNonAdminListQueries() {
        OssFileQueryForm form = new OssFileQueryForm();
        form.setUploaderId(99L);
        Page<SysOssFile> page = new Page<>(1, 10);
        when(ossFileService.listFiles(any(), any())).thenReturn(page);
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE)).thenReturn(false);
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);
            service.list(form, new PageParam(1, 10));
        }

        ArgumentCaptor<OssFileQuery> queryCaptor = ArgumentCaptor.forClass(OssFileQuery.class);
        verify(ossFileService).listFiles(queryCaptor.capture(), any(PageParam.class));
        assertThat(queryCaptor.getValue().getUploaderId()).isEqualTo(10L);
    }

    @Test
    void preservesTheOptionalUploaderFilterOnAdminListQueries() {
        OssFileQueryForm form = new OssFileQueryForm();
        form.setUploaderId(99L);
        Page<SysOssFile> page = new Page<>(1, 10);
        when(ossFileService.listFiles(any(), any())).thenReturn(page);
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE)).thenReturn(true);
            service.list(form, new PageParam(1, 10));
        }

        ArgumentCaptor<OssFileQuery> queryCaptor = ArgumentCaptor.forClass(OssFileQuery.class);
        verify(ossFileService).listFiles(queryCaptor.capture(), any(PageParam.class));
        assertThat(queryCaptor.getValue().getUploaderId()).isEqualTo(99L);
    }

    @Test
    void allowsUserToDeleteOwnFile() {
        SysOssFile file = storedFile(1L, 10L);
        when(ossFileService.getById(1L)).thenReturn(file);
        when(ossTemplate.delete(any(FileInfo.class))).thenReturn(true);
        when(ossFileService.removeById(1L)).thenReturn(true);
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE)).thenReturn(false);
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            service.deleteById(1L);
        }

        verify(sysUserService).isInUse(file.getFileUrl());
        verify(ossTemplate).delete(any(FileInfo.class));
        verify(ossFileService).removeById(1L);
    }

    @Test
    void rejectsDeletingAFileUsedAsAnAvatarEvenForAdmin() {
        SysOssFile file = storedFile(1L, 20L);
        when(ossFileService.getById(1L)).thenReturn(file);
        when(sysUserService.isInUse(file.getFileUrl())).thenReturn(true);
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE)).thenReturn(true);

            assertThatThrownBy(() -> service.deleteById(1L))
                    .hasMessage(FileConstants.FILE_AVATAR_IN_USE_MESSAGE);
        }

        verify(sysUserService).isInUse(file.getFileUrl());
        verify(ossTemplate, never()).delete(any(FileInfo.class));
        verify(ossFileService, never()).removeById(any());
    }

    @Test
    void rejectsDeletingAnotherUsersFile() {
        when(ossFileService.getById(1L)).thenReturn(storedFile(1L, 20L));
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE)).thenReturn(false);
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            assertThatThrownBy(() -> service.deleteById(1L))
                    .hasMessage(FileConstants.FILE_NOT_FOUND_OR_FORBIDDEN_MESSAGE);
        }

        verify(sysUserService, never()).isInUse(anyString());
        verify(ossTemplate, never()).delete(any(FileInfo.class));
        verify(ossFileService, never()).removeById(any());
    }

    @Test
    void reportsTheSameMessageWhenDeletingAMissingFile() {
        when(ossFileService.getById(1L)).thenReturn(null);
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        assertThatThrownBy(() -> service.deleteById(1L))
                .hasMessage(FileConstants.FILE_NOT_FOUND_OR_FORBIDDEN_MESSAGE);

        verify(sysUserService, never()).isInUse(anyString());
        verify(ossTemplate, never()).delete(any(FileInfo.class));
    }

    @Test
    void allowsAdminToDeleteAnotherUsersFile() {
        when(ossFileService.getById(1L)).thenReturn(storedFile(1L, 20L));
        when(ossTemplate.delete(any(FileInfo.class))).thenReturn(true);
        when(ossFileService.removeById(1L)).thenReturn(true);
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE)).thenReturn(true);

            service.deleteById(1L);
        }

        verify(ossTemplate).delete(any(FileInfo.class));
        verify(ossFileService).removeById(1L);
    }

    @Test
    void rejectsDownloadingAnotherUsersFileWithTheUnifiedMessage() {
        when(ossFileService.getById(1L)).thenReturn(storedFile(1L, 20L));
        FileBizService service = new FileBizService(fileUploadValidator, ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE)).thenReturn(false);
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            assertThatThrownBy(() -> service.download(1L, new MockHttpServletResponse()))
                    .hasMessage(FileConstants.FILE_NOT_FOUND_OR_FORBIDDEN_MESSAGE);
        }

        verify(ossTemplate, never()).getFileStorageService();
    }

    private static MockMultipartFile file() {
        return multipartFile("avatar.png", pngBytes());
    }

    private static MockMultipartFile multipartFile(String filename, byte[] content) {
        return new MockMultipartFile("file", filename, "application/octet-stream", content);
    }

    private static Stream<Arguments> allowedUploads() {
        return Stream.of(
                Arguments.of("photo.jpg", "image/jpeg", jpegBytes()),
                Arguments.of("photo.jpeg", "image/jpeg", jpegBytes()),
                Arguments.of("image.png", "image/png", pngBytes()),
                Arguments.of("animation.gif", "image/gif", gifBytes()),
                Arguments.of("image.webp", "image/webp", webpBytes()),
                Arguments.of("video-isom.mp4", "video/mp4", ftypBytes("isom")),
                Arguments.of("video-mp41.mp4", "video/mp4", ftypBytes("mp41")),
                Arguments.of("video-mp42.mp4", "video/mp4", ftypBytes("mp42")),
                Arguments.of("video-iso2.mp4", "video/mp4", ftypBytes("iso2")),
                Arguments.of("video-iso4.mp4", "video/mp4", ftypBytes("iso4")),
                Arguments.of("video-iso5.mp4", "video/mp4", ftypBytes("iso5")),
                Arguments.of("video-iso6.mp4", "video/mp4", ftypBytes("iso6")),
                Arguments.of("video-avc1.mp4", "video/mp4", ftypBytes("avc1")),
                Arguments.of("video-dash.mp4", "video/mp4", ftypBytes("dash")),
                Arguments.of("video-msnv.mp4", "video/mp4", ftypBytes("MSNV")),
                Arguments.of("video-ndas.mp4", "video/mp4", ftypBytes("NDAS")),
                Arguments.of("video-xavc.mp4", "video/mp4", ftypBytes("XAVC")),
                Arguments.of("video-m4v.mp4", "video/mp4", ftypBytes("M4V ")),
                Arguments.of("video-m4vh.mp4", "video/mp4", ftypBytes("M4VH")),
                Arguments.of("video-m4vp.mp4", "video/mp4", ftypBytes("M4VP")),
                Arguments.of("document.pdf", "application/pdf", pdfBytes()),
                Arguments.of("archive.zip", "application/zip", zipBytes()),
                Arguments.of("notes.txt", "text/plain", textBytes())
        );
    }

    private static Stream<String> nonMp4FtypBrands() {
        return Stream.of("qt  ", "avif", "heic", "3gp6", "M4A ");
    }

    private static byte[] jpegBytes() {
        return new byte[]{
                (byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0,
                0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01
        };
    }

    private static byte[] pngBytes() {
        return new byte[]{
                (byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a
        };
    }

    private static byte[] gifBytes() {
        return "GIF89a".getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] webpBytes() {
        return new byte[]{
                'R', 'I', 'F', 'F', 0x04, 0x00, 0x00, 0x00,
                'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
        };
    }

    private static byte[] ftypBytes(String majorBrand) {
        byte[] brand = majorBrand.getBytes(StandardCharsets.US_ASCII);
        if (brand.length != 4) {
            throw new IllegalArgumentException("FTYP major brand must contain four ASCII bytes");
        }
        return new byte[]{
                0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p',
                brand[0], brand[1], brand[2], brand[3], 0x00, 0x00, 0x02, 0x00,
                'i', 's', 'o', 'm', 'i', 's', 'o', '2'
        };
    }

    private static byte[] pdfBytes() {
        return "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] zipBytes() {
        return new byte[]{
                'P', 'K', 0x05, 0x06,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00
        };
    }

    private static byte[] textBytes() {
        return "Nexora upload test".getBytes(StandardCharsets.UTF_8);
    }

    private static OssUploadResult uploadResult(String id) {
        return OssUploadResult.builder()
                .id(id)
                .url("https://oss.example.com/file.png")
                .filename("file.png")
                .originalFilename("avatar.png")
                .contentType("image/png")
                .size(3L)
                .platform("qiniu-kodo-1")
                .build();
    }

    private static SysOssFile storedFile(Long id, Long uploaderId) {
        return SysOssFile.builder()
                .id(id)
                .fileId("file-" + id)
                .fileUrl("https://oss.example.com/file.png")
                .fileName("file.png")
                .platform("qiniu-kodo-1")
                .uploaderId(uploaderId)
                .build();
    }
}
