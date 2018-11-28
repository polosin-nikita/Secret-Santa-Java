package ru.polosin;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;


public class SecretSanta {
    private Integer groupId;
    private String accessToken = "35482a0475db2306458e0fc484fd120a17160aa6f2e06ec6f47ce3c8ca73f8fa97b6ef01796c289b76242";
    private List<Member> members;
    private Boolean verification = true;

    private SecretSanta() {
    }


    public class Member {
        Integer id;
        String contact;
        int santa;
        int spouse;

        private Member(Integer id, String contact, int spouse) {
            this.id = id;
            this.contact = contact;
            this.spouse = spouse;
        }

    }

    private String makeUrlRequest(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();

        InputStream is = connection.getInputStream();
        InputStreamReader reader = new InputStreamReader(is);
        char[] buffer = new char[256];
        int rc;
        StringBuilder sb = new StringBuilder();
        while ((rc = reader.read(buffer)) != -1)
            sb.append(buffer, 0, rc);
        reader.close();
        return sb.toString();
    }

    private void addMembers(Integer groupId) throws IOException {
        this.groupId = groupId;

        Map<Integer, Integer> spouses = new HashMap<Integer, Integer>();
        spouses.put(8534791, 16737838);
        spouses.put(9563896, 21776989);
        spouses.put(22023306, 517200463);

        Map<String, Integer> users = new HashMap<String, Integer>();
        String url = "https://api.vk.com/method/groups.getMembers?group_id="
                + groupId + "&fields=contacts&access_token=" + accessToken + "&v=5.87";
        JSONObject obj = new JSONObject(makeUrlRequest(url));
        JSONArray membersArray = obj.getJSONObject("response").getJSONArray("items");
        List<Member> members = new ArrayList<Member>();

        for (Object member : membersArray) {
            Integer spouse = 0;
            JSONObject memberJSON = new JSONObject(member.toString());
            for (Map.Entry<Integer,Integer> sp : spouses.entrySet()){
                if(memberJSON.getInt("id") == sp.getKey()) spouse = sp.getValue();
                if(memberJSON.getInt("id") == sp.getValue()) spouse = sp.getKey();

            }
            members.add(new Member(memberJSON.getInt("id"), memberJSON.getString("last_name") + " " + memberJSON.getString("first_name"), spouse));
        }
        System.out.println("Всего участников: " + members.size());
        this.members = members;
    }

    private void verifyMembers(SecretSanta secretSanta) throws IOException {

        for (Member member : secretSanta.members) {
            String url = "https://api.vk.com/method/messages.isMessagesFromGroupAllowed?group_id="
                    + secretSanta.groupId + "&user_id=" + member.id + "&access_token=" + accessToken + "&v=5.87";
            JSONObject result = new JSONObject(makeUrlRequest(url));
            int isMessagesFromGroupAllowed = result.getJSONObject("response").getInt("is_allowed");
            if (isMessagesFromGroupAllowed == 0) {
                System.out.println("Messages from Group isn't allowed for " + member.id + " " + member.contact);
                this.verification = false;
            }


        }

    }

    private void selectSanta() {
        boolean attempt = true;
        Random random = new Random();
        int numAttempt = 1;
        //Create array of santa
        System.out.println("Попытка выбрать Санту:");
        while (attempt) {
            try {
                int[] santas = new int[this.members.size()];
                int i = 0;
                for (Member member : this.members) {
                    santas[i] = member.id;
                    i++;
                }

                int[] memberIdFSTemp = Arrays.copyOf(santas, santas.length);
                for (Member member : this.members) {
                    int[] memberIdFS = new int[memberIdFSTemp.length];
                    i = 0;
                    for (int memberFS : memberIdFSTemp) {
                        if (memberFS != member.id && memberFS != member.spouse) {
                            memberIdFS[i] = memberFS;
                            i++;

                        }

                    }
                    member.santa = memberIdFS[random.nextInt(memberIdFS.length)];
                    int k = 0;
                    int[] memberIdFSClone = new int[memberIdFSTemp.length - 1];
                    for (int memberFS : memberIdFSTemp) {
                        if (member.santa != memberFS) {
                            memberIdFSClone[k] = memberFS;
                            k++;
                        }
                    }
                    memberIdFSTemp = Arrays.copyOf(memberIdFSClone, memberIdFSClone.length);
                }
                attempt = false;
//System.out.println(this.members);
            } catch (ArrayIndexOutOfBoundsException ex) {
                attempt = true;
                System.out.println("Attempt " + numAttempt + " is failed. We are trying again now...");
                numAttempt++;
            }
        }
        System.out.println("Санта выбран");

    }

    private void send() throws IOException {
        for (Member member : this.members) {
            String urlSantaInfo = "https://api.vk.com/method/users.get?user_id=" + member.santa + "&fields=contacts&access_token=362662e4758a62725f593f6c6c27aaa86892f428bbf135b6a42a726862ee4380039df6147dcd66105f2e4&v=5.87";
            JSONObject obj = new JSONObject(makeUrlRequest(urlSantaInfo));
            JSONObject objContact = obj.getJSONArray("response").getJSONObject(0);
            String nameSanta = objContact.getString("first_name");
            urlSantaInfo = "https://api.vk.com/method/users.get?user_id=" + member.id + "&fields=contacts&name_case=gen&access_token=362662e4758a62725f593f6c6c27aaa86892f428bbf135b6a42a726862ee4380039df6147dcd66105f2e4&v=5.87";
            obj = new JSONObject(makeUrlRequest(urlSantaInfo));
            objContact = obj.getJSONArray("response").getJSONObject(0);

            String nameUserForSanta = objContact.getString("last_name") + " " + objContact.getString("first_name");
            String text = "Хо-Хо-Хо<br> Приветствую тебя, " + nameSanta + "!<br>Дух Нового года сделал свой выбор.<br>Ты Тайный Санта для " + nameUserForSanta + "<br>Хо-Хо-Хо! С наступающим 2019 Новым годом!!!";
            System.out.println(text);
            String url = "https://api.vk.com/method/messages.send?user_id=" + member.santa + "&attachment=photo-174027864_456239024,audio-174027864_456239017&message=" + text.replace(" ", "+") + "&access_token=" + this.accessToken + "&v=5.87";
            System.out.println(url);
            makeUrlRequest(url);
        }
    }

    public static void main(String[] args) throws IOException {

        SecretSanta secretSanta = new SecretSanta();
        System.out.println("Добавление участников Тайного санты");
        secretSanta.addMembers(174027864);
        for (Member member : secretSanta.members) {
            System.out.println(member.id + " " + member.contact);
        }
        System.out.println("Проверка возможности отправлять сообщения участникам");
        secretSanta.verifyMembers(secretSanta);
        if (secretSanta.verification) {
            System.out.println("Проверка прошла успешно.");
            secretSanta.selectSanta();


            for (Member member : secretSanta.members) {
                System.out.println(member.id + " " + member.contact + " " + member.santa + " " + member.spouse);
            }
            secretSanta.send();
        } else {
            System.out.println("ERROR: Не все участники разрешили присылать им сообщение");
        }

    }
}