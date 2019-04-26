package cn.zyx.params;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/user")
public class ParamsController01 {
    @RequestMapping("/params01")
    public ModelAndView getParams01(String username , int userage)throws Exception{
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("username",username);
        modelAndView.addObject("userage",userage);
        modelAndView.addObject("params01test","params01test");
        modelAndView.setViewName("result");

        return modelAndView;
    }
}
