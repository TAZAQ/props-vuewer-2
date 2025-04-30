# PropsVuewer - плагин для IntelliJ IDEA

## Описание
PropsVuewer - это плагин для IntelliJ IDEA, который отображает inline свойства (props) в компонентах Vue 2, добавленных с помощью оператора распространения (spread) или при назначении импортированных props.

## Функциональность
Плагин показывает props в двух основных сценариях:

### Сценарий 1: Использование оператора spread
```vue
props: {
  ...MLazyListBaseProps, // показывает подсказки для распространенных props
  list: { type: Object, required: true },
},
