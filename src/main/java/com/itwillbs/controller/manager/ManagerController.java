package com.itwillbs.controller.manager;

import com.itwillbs.config.security.util.SecurityUtil;
import com.itwillbs.entity.Manager;
import com.itwillbs.service.ManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Log
public class ManagerController {
    private final ManagerService managerService;


    @GetMapping("/login")
    public String login(){
        log.info("AdminsController login()");
        return "managers/login";
    }

    @GetMapping("/manager/list")
    public String managerList(Model model,
                              @RequestParam(value = "page", defaultValue = "1", required = false)int page,
                              @RequestParam(value = "size", defaultValue = "3", required = false)int size){
        log.info("AdminsController managers()");

        Pageable pageable = PageRequest.of(page-1, size);
        Page<Manager> managers = managerService.getManagerList(pageable);

        model.addAttribute("managers",managers);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        //전체 페이지 개수
        model.addAttribute("totalPages", managers.getTotalPages());

        int pageBlock =5;
        int startPage = (page-1)/pageBlock * pageBlock + 1;
        int endPage = startPage + pageBlock - 1;
        if(endPage > managers.getTotalPages()) {
            endPage = managers.getTotalPages();
        }
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "managers/managers";
    }
    @GetMapping("error/403")
    public String error403() {
        return "/error/403error";
    }

    @GetMapping("error/401")
    public String error401() {
        return "/error/401error";
    }
}
