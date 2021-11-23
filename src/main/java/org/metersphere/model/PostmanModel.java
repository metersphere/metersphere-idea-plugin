package org.metersphere.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PostmanModel implements Serializable {

    private String name;
    private String description;
    private List<ItemBean> item;

    @Data
    public static class ItemBean implements Serializable {

        private RequestBean request;
        private String name;
        private List<ResponseBean> response;

        @Data
        public static class RequestBean implements Serializable {

            private String method;
            private BodyBean body;
            private UrlBean url;
            private List<HeaderBean> header;

            @Data
            public static class BodyBean implements Serializable {

                private String mode;
                private OptionsBean options;
                private String raw;
                private List<FormDataBean> formdata;

                @Data
                @AllArgsConstructor
                public static class FormDataBean implements Serializable {
                    private String key;
                    private String type;
                    private Object value;
                    private String description;
                }

                @Data
                public static class OptionsBean implements Serializable {

                    private RawBean raw;

                    @Data
                    public static class RawBean implements Serializable {

                        private String language;
                    }
                }
            }

            @Data
            public static class UrlBean implements Serializable {

                private String host;
                private String raw;
                private List<String> path;
                private List<?> query;
                private List<?> variable;
            }

            @Data
            public static class HeaderBean implements Serializable {

                private String key;
                private String value;
                private String type;
                private String description;
            }
        }

        @Data
        public static class ResponseBean implements Serializable {

            private OriginalRequestBean originalRequest;
            private String _postman_previewlanguage;
            private int code;
            private String _postman_previewtype;
            private int responseTime;
            private String name;
            private String body;
            private String status;
            private List<HeaderBeanXX> header;

            @Data
            public static class OriginalRequestBean implements Serializable {

                private String method;
                private BodyBeanX body;
                private UrlBeanX url;
                private List<HeaderBeanX> header;

                @Data
                public static class BodyBeanX implements Serializable {

                    private String mode;
                    private OptionsBeanX options;
                    private String raw;

                    @Data
                    public static class OptionsBeanX implements Serializable {

                        private RawBeanX raw;

                        @Data
                        public static class RawBeanX implements Serializable {

                            private String language;
                        }
                    }
                }

                @Data
                public static class UrlBeanX implements Serializable {

                    private String host;
                    private List<?> query;
                }

                @Data
                public static class HeaderBeanX implements Serializable {

                    private String key;
                    private String value;
                    private String type;
                    private String description;
                }
            }

            @Data
            public static class HeaderBeanXX implements Serializable {

                private String name;
                private String key;
                private String value;
                private String description;
            }
        }
    }
}
