package com.cos.blog.controller.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cos.blog.config.auth.PrincipalDetail;
import com.cos.blog.dto.ResponseDto;
import com.cos.blog.model.Board;
import com.cos.blog.model.Gpt;
import com.cos.blog.model.User;
import com.cos.blog.service.TravelPlanService;
import com.cos.blog.service.UserService;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
public class UserApiController {

    @Autowired
    private TravelPlanService travelPlanService;
    
    @Autowired // DI 주입
    private UserService userService;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @PostMapping("/auth/joinProc")
    public ResponseDto<Integer> save(@Valid @RequestBody User user) { // username, password, email
        System.out.println("UserApiController:save 호출됨");
        userService.회원가입(user);
        return new ResponseDto<Integer>(HttpStatus.OK.value(), 1); // 자바오브젝트를 JSON으로 변환해서 리턴(Jackson), OK = 정상적이면 200, result = 정상이면 1 아니면 -1 (Userservice)
    }
    
    // user.js에서 만든 수정을 위한 URL "/user" Controller 만들기
    @PutMapping("/user")
    public ResponseDto<Integer> update(@Valid @RequestBody User user){ //@RequestBody는 json데이터 받고 싶으면 쓰는 것, 안쓰면 key=value, x-www-from-urlencoded 를 받음
        userService.회원수정(user);
        // 여기서는 트랜잭션이 종료되기 때문에 DB의 값은 변경이 됨
        // 하지만 세션값은 변경이 되지 않은 상태이기 때문에 직접 세션값을 변경해줄 것임
        // 세션 등록
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        return new ResponseDto<Integer>(HttpStatus.OK.value(),1);
    }
    
    @PostMapping("/api/user/travel/plan")
    public Mono<ResponseEntity<Map<String, String>>> createTravelPlan(@Valid @RequestBody Map<String, String> requestData) {
        String start = requestData.get("start");
        String end = requestData.get("end");
        String days = requestData.get("days");
        String content = "출발지는 " + start + "이고 도착지는 " + end + " 이고, " + days + "일 동안 여행을 갈 건데 여행계획을 작성해줘";

        return travelPlanService.여행계획(content)
                .map(plan -> {
                    Map<String, String> response = new HashMap<>();
                    response.put("plan", plan);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                })
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }
    
    @PostMapping("/api/user/travel/save")
    public ResponseDto<Integer> planSave(@RequestBody Gpt gpt, @AuthenticationPrincipal PrincipalDetail principal) {
    	travelPlanService.계획저장(gpt, principal.getUser());
		return new ResponseDto<Integer>(HttpStatus.OK.value(), 1);
    }
    
    @PutMapping("/api/user/planUpdate/{id}")
	public ResponseDto<Integer> planUpdate(@Valid @PathVariable int id, @RequestBody Gpt gpt) {
    	System.out.println(gpt);
	    travelPlanService.글수정하기(id, gpt);
	    return new ResponseDto<Integer>(HttpStatus.OK.value(), 1);
	}
    
    @DeleteMapping("/api/user/{id}")
	public ResponseDto<Integer> planDelete(@PathVariable int id) {
    	travelPlanService.글삭제하기(id);
		return new ResponseDto<Integer>(HttpStatus.OK.value(), 1);
	}
   
}
