package com.cos.blog.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.cos.blog.model.Board;
import com.cos.blog.model.Gpt;
import com.cos.blog.model.RoleType;
import com.cos.blog.model.User;
import com.cos.blog.repository.BoardRepository;
import com.cos.blog.repository.GptRepository;
import com.cos.blog.repository.ReplyRepository;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
public class TravelPlanService {

    private final WebClient webClient;

    public TravelPlanService(WebClient.Builder webClientBuilder, @Value("${openai.api.key}") String apiKey) {
        this.webClient = webClientBuilder
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    public Mono<String> 여행계획(String content) {
        return webClient.post()
            .uri("/chat/completions")
            .bodyValue(Map.of(
                "model", "gpt-3.5-turbo-16k",
                "messages", List.of(
                    Map.of("role", "system", "content", "You are a travel guide; when I provide the departure city, destination city, and number of travel days, please recommend suitable transportation options, attractions, activities, and interesting events at the destination, such as affordable flights, major attractions, interesting events, and popular restaurants based on the given information.또한 추천 장소를 종합하여 계획도 시간별로 작성해줘."),
                    Map.of("role", "user", "content", content)
                )
            ))
            .retrieve()
            .bodyToMono(Map.class) // JSON 응답을 Map으로 변환
            .flatMap(response -> {
                // JSON 응답에서 'content' 필드만 추출
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String resultContent = (String) message.get("content"); // 'choices' 배열의 첫 번째 요소에서 'message' 객체 내의 'content' 필드를 추출
                    return Mono.just(resultContent);
                } else {
                    return Mono.error(new RuntimeException("Invalid response format"));
                }
            });
    }
    
    @Autowired
    private GptRepository gptRepository;
    
    @Transactional
    public void 계획저장(Gpt gpt, User user) {
        gpt.setUser(user);
        gpt.setStart(gpt.getStart());
        gpt.setEnd(gpt.getEnd());
        gpt.setDays(gpt.getDays());
        gpt.setResult(gpt.getResult());
        gptRepository.save(gpt);
        gptRepository.save(gpt);
    }
    
    @Transactional(readOnly = true)
	public Page<Gpt> 사용자별글목록(String username, Pageable pageable){
		return gptRepository.findByUserUsername(username, pageable);
	}
    
    @Transactional(readOnly = true)
	public Gpt 글상세보기(int id) {
		return gptRepository.findById(id)
				.orElseThrow(()->{
					return new IllegalArgumentException("글 상세보기 실패: 아이디를 찾을 수 없습니다.");
				});
	}
    
    @Transactional
	public void 글수정하기(int id, Gpt requestGpt) {
		Gpt gpt = gptRepository.findById(id)
				.orElseThrow(()->{
					return new IllegalArgumentException("글 찾기 실패: 아이디를 찾을 수 없습니다.");
				});	// 영속화 완료
		System.out.println(requestGpt);
		gpt.setResult(requestGpt.getResult());
	}
    
    @Transactional
	public void 글삭제하기(int id) {
		gptRepository.deleteById(id);
	}

}
