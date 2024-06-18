package org.xu.newjob.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.xu.newjob.annotation.LoginRequired;
import org.xu.newjob.entity.User;
import org.xu.newjob.service.LikeService;
import org.xu.newjob.service.UserService;
import org.xu.newjob.util.CookieUtil;
import org.xu.newjob.util.HostHolder;
import org.xu.newjob.util.NewJobUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${newJob.path.upload}")
    private String uploadPath;
    @Value("${newJob.path.domain}")
    private String domain;
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private LikeService likeService;

    @PostConstruct // 构造函数之后执行
    private void init() {
        // 自动创建文件的路径（为简化部署）
        File dir = new File(uploadPath + "/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }


    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage() {
        return "/site/setting";
    }

    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("headerError", "您还没有选择图片！");
            return "/site/setting";
        }

        String filename = headerImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf('.'));
        if (!".png".equals(suffix) && !".jpg".equals(suffix) && !".jpeg".equals(suffix)) {
            model.addAttribute("headerError", "图片仅支持 .jpg .png .jpeg 格式！");
            return "/site/setting";
        }

        // 生成随机文件名
        filename = NewJobUtil.generateUUID() + suffix;
        // 确定文件存放的路径
        File dest = new File(uploadPath + "/" + filename);

        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败: " + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常！", e);
        }

        // 更新当前用户头像的路径
        // http://localhost:8080/newJob/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + filename;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }

    @RequestMapping(path = "/header/{filename}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("filename") String filename, HttpServletResponse response) {
        // 服务器存放路径
        filename = uploadPath + "/" + filename;
        // 文件后缀
        String suffix = filename.substring(filename.lastIndexOf('.'));
        // 响应图片
        response.setContentType("image/" + suffix);
        try (
                FileInputStream fis = new FileInputStream(filename);
                ServletOutputStream os = response.getOutputStream()
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败: " + e.getMessage());
        }
    }

    @LoginRequired
    @RequestMapping(path = "/password", method = RequestMethod.POST)
    public String updatePassword(String oldPassword, String newPassword, Model model, HttpServletRequest request) {
        Map<String, Object> map = userService.updatePassword(oldPassword, newPassword);
        if (map != null && !map.isEmpty()) {
            model.addAttribute("oldPasswordError", map.get("oldPasswordError"));
            model.addAttribute("newPasswordError", map.get("newPasswordError"));
            model.addAttribute("oldPassword", oldPassword);
            model.addAttribute("newPassword", newPassword);
            return "/site/setting";
        }
        model.addAttribute("msg", "密码修改成功，请重新登录！");
        model.addAttribute("target", "/login");
        String ticket = CookieUtil.getValue(request, "ticket");
        userService.logout(ticket);
        hostHolder.clear();
        return "/site/operate-result";
    }

    @RequestMapping(value = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        model.addAttribute("user", user);
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);
        return "/site/profile";
    }
}
