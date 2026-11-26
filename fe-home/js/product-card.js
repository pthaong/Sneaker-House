
/*
 * Sneaker House - Product Card Component
 * Dung chung cho trang chu va trang danh muc.
 * Giu nguyen thiet ke pcard da duyet.
 */

(function (global) {
  'use strict';

  function render(raw, deps) {
    const {
      h,
      icon,
      norm,
      productHref,
      money,
      stars,
      toggleWish,
      addToCart,
      flash = false
    } = deps;

    const p = norm(raw);
    const href = productHref(p);

    const wish = h(
      'button',
      {
        class: 'wish',
        type: 'button',
        'aria-pressed': 'false',
        'aria-label': `Thêm ${p.name} vào yêu thích`,
        onclick: () => toggleWish(p, wish)
      },
      icon('heart')
    );

    const media = h(
      'a',
      {
        class: 'pcard__media',
        href,
        tabindex: '-1',
        'aria-hidden': 'true'
      },
      p.image
        ? h('img', {
            src: p.image,
            alt: '',
            loading: 'lazy',
            width: 720,
            height: 900,
            onerror: (e) => {
              e.target.remove();
            }
          })
        : null,

      p.percent
        ? h('span', { class: 'badge' }, `-${p.percent}%`)
        : null,

      !p.inStock
        ? h('span', { class: 'badge badge--stock' }, 'Hết hàng')
        : null
    );

    const body = h(
      'div',
      { class: 'pcard__body' },

      p.brand
        ? h('span', { class: 'pcard__brand' }, p.brand)
        : null,

      h('a', { class: 'pcard__name', href }, p.name),

      h(
        'div',
        { class: 'price' },
        h('b', null, money(p.sale || p.price)),
        p.sale ? h('s', null, money(p.price)) : null
      ),

      p.reviews > 0
        ? h(
            'div',
            { class: 'rate' },
            stars(p.rating),
            `(${p.reviews})`
          )
        : null
    );

    if (flash && p.stock != null && p.sold != null) {
      const total = p.stock + p.sold || 1;

      body.append(
        h(
          'div',
          {
            class: 'bar',
            role: 'img',
            'aria-label': `Đã bán ${p.sold}`
          },
          h('i', {
            style: `width:${Math.min(
              100,
              Math.round((p.sold / total) * 100)
            )}%`
          })
        ),
        h(
          'span',
          { class: 'stockline' },
          p.inStock
            ? `Đã bán ${p.sold} · còn ${p.stock}`
            : 'Đã hết hàng'
        )
      );
    }

    body.append(
      h(
        'div',
        { class: 'pcard__act' },

        h(
          'a',
          { class: 'btn btn--ghost', href },
          'Xem chi tiết'
        ),

        h(
          'button',
          {
            class: 'btn',
            type: 'button',
            disabled: !p.inStock,
            onclick: () => addToCart(p)
          },
          icon('cartPlus'),
          'Thêm vào giỏ'
        )
      )
    );

    return h(
      'article',
      {
        class: `pcard${p.inStock ? '' : ' is-out'}`
      },
      media,
      wish,
      body
    );
  }

  global.SneakerProductCard = {
    render
  };

})(window);
