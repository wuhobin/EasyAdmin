package com.nexora.identity.controller;

import com.nexora.identity.biz.WechatLoginService;
import com.nexora.identity.infrastructure.WechatMpClientManager;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/wechat/portal")
@RequiredArgsConstructor
public class WechatMpPortalController {

    private final WechatMpClientManager clientManager;
    private final WechatLoginService wechatLoginService;

    @GetMapping(produces = "text/plain;charset=utf-8")
    public String verify(
            @RequestParam String signature,
            @RequestParam String timestamp,
            @RequestParam String nonce,
            @RequestParam String echostr) {
        return clientManager.service().checkSignature(timestamp, nonce, signature)
                ? echostr : "非法请求";
    }

    @PostMapping(produces = "application/xml;charset=UTF-8")
    public String receive(
            @RequestBody String requestBody,
            @RequestParam String signature,
            @RequestParam String timestamp,
            @RequestParam String nonce,
            @RequestParam(name = "encrypt_type", required = false) String encryptType,
            @RequestParam(name = "msg_signature", required = false) String msgSignature) {
        WxMpService service = clientManager.service();
        if (!service.checkSignature(timestamp, nonce, signature)) {
            return "";
        }
        boolean encrypted = "aes".equalsIgnoreCase(encryptType);
        WxMpXmlMessage message = encrypted
                ? WxMpXmlMessage.fromEncryptedXml(
                        requestBody, service.getWxMpConfigStorage(), timestamp, nonce, msgSignature)
                : WxMpXmlMessage.fromXml(requestBody);
        if (!"text".equalsIgnoreCase(message.getMsgType())) {
            return "";
        }
        String reply = wechatLoginService.acceptCode(message.getContent(), message.getFromUser());
        WxMpXmlOutMessage out = WxMpXmlOutMessage.TEXT()
                .content(reply)
                .fromUser(message.getToUser())
                .toUser(message.getFromUser())
                .build();
        return encrypted ? out.toEncryptedXml(service.getWxMpConfigStorage()) : out.toXml();
    }
}
